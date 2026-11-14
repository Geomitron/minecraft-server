// Turns what the server leaves behind (log lines, crash reports, Java crash files, exit records) into error events.
// An event has a short id, a kind, a source (the "process" label) and one sample line with player names and
// addresses blanked. Nothing here touches the filesystem.

import { createHash } from 'node:crypto';

const LOG_HEAD = /^\[(\d\d:\d\d:\d\d)\] \[(.+?)\/(TRACE|DEBUG|INFO|WARN|ERROR|FATAL)\]:? ?(.*)$/;
const EXCEPTION_CLASS = /^((?:[A-Za-z_$][\w$]*\.)+[A-Za-z_$][\w$]*)(?::|$)/;
const SAMPLE_MAX = 500;
const PROCESS_MAX = 80;

/** Log ERROR records that say nothing about the server, by the reason they're ignored. */
const IGNORED = [
  {
    reason: 'scanner-disconnect',
    test: (message) => message.startsWith('Error sending packet clientbound/minecraft:disconnect'),
  },
  { reason: 'invalid-session', test: (message) => /^Username '.*' tried to join with an invalid session$/.test(message) },
];

export const IGNORED_REASONS = IGNORED.map((rule) => rule.reason);

export function shortId(...parts) {
  return createHash('sha256').update(parts.join('\0')).digest('hex').slice(0, 8);
}

function escapeRegExp(text) {
  return text.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}

/**
 * Blanks player names and addresses. `playerNames` are the names the server knows (whitelist, operators, user
 * cache); names it doesn't know are caught where the log quotes them or a game profile names them.
 */
export function makeRedactor(playerNames) {
  const names = [...new Set(playerNames.filter((name) => typeof name === 'string' && name.length >= 3))];
  names.sort((a, b) => b.length - a.length);
  const namePattern = names.length
    ? new RegExp(`(?<![A-Za-z0-9_])(?:${names.map(escapeRegExp).join('|')})(?![A-Za-z0-9_])`, 'gi')
    : null;

  /** The sample line: names, profile ids and addresses blanked, everything else kept. */
  function sample(text) {
    let out = text;
    if (namePattern) out = out.replace(namePattern, '<player>');
    out = out
      .replace(/\b(name=)'?[^,'}\]\s]+'?/g, '$1<player>')
      .replace(/\b(Username |[Pp]layer )'[^']*'/g, "$1'<player>'")
      .replace(/\b[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}\b/g, '<uuid>')
      .replace(/\b(?:\d{1,3}\.){3}\d{1,3}(?::\d{1,5})?\b/g, '<address>')
      // IPv6: a "::" or at least four groups, so times like 10:00:01 stay.
      .replace(/\[(?=[0-9a-fA-F:]*::|(?:[0-9a-fA-F]{1,4}:){3})[0-9a-fA-F:]+\](?::\d{1,5})?/g, '<address>')
      .replace(/(?<![\w:.])(?=[0-9a-fA-F:]*::|(?:[0-9a-fA-F]{1,4}:){3})[0-9a-fA-F]{0,4}(?::[0-9a-fA-F]{0,4}){2,7}(?![\w:])/g, '<address>');
    return out.length > SAMPLE_MAX ? `${out.slice(0, SAMPLE_MAX)}…` : out;
  }

  /** The text an id is made from: the sample with numbers, object hashes and hex runs blanked too. */
  function key(text) {
    return sample(text)
      .replace(/@[0-9a-fA-F]+\b/g, '@<hash>')
      .replace(/\b(?=[0-9a-fA-F]*\d)(?=[0-9a-fA-F]*[a-fA-F])[0-9a-fA-F]{6,}\b/g, '<hex>')
      .replace(/-?\d+(?:\.\d+)?/g, '<n>');
  }

  return { sample, key };
}

/** A thread name as a stable source: numbered pool threads collapse into one. */
export function processFromThread(thread) {
  const cleaned = thread.replace(/\d+/g, 'N').replace(/[^\x20-\x7e]/g, '?');
  return cleaned.slice(0, PROCESS_MAX) || 'unknown';
}

/**
 * Splits log bytes into records: a head line and the first line after it, when that line continues the record (a
 * stack trace). Only complete lines are used. `start`/`end` are byte offsets relative to `buffer`.
 */
export function splitLogRecords(buffer) {
  const records = [];
  let position = 0;
  let current = null;
  while (position < buffer.length) {
    const newline = buffer.indexOf(0x0a, position);
    if (newline === -1) break;
    const line = buffer.toString('utf8', position, newline).replace(/\r$/, '');
    const head = LOG_HEAD.exec(line);
    if (head) {
      current = { start: position, end: newline + 1, thread: head[2], level: head[3], message: head[4], next: null };
      records.push(current);
    } else if (current) {
      if (current.next === null) current.next = line;
      current.end = newline + 1;
    }
    position = newline + 1;
  }
  return { records, consumed: position };
}

/**
 * Classifies one log record: null for anything but ERROR and FATAL, `{ ignored }` for the excluded kinds, or an
 * event observation.
 */
export function classifyLogRecord(record, redactor) {
  if (record.level !== 'ERROR' && record.level !== 'FATAL') return null;
  const rule = IGNORED.find((candidate) => candidate.test(record.message));
  if (rule) return { ignored: rule.reason };
  const process = processFromThread(record.thread);
  const exception = record.next === null ? '' : (EXCEPTION_CLASS.exec(record.next.trim())?.[1] ?? '');
  return {
    id: shortId('log-error', process, redactor.key(record.message), exception),
    kind: 'log-error',
    process,
    sample: redactor.sample(exception ? `${record.message} | ${exception}` : record.message),
  };
}

/** A crash report: the watchdog's stopping of a stuck server is its own kind. `name` is the report's file name. */
export function classifyCrashReport(name, text, redactor) {
  const lines = text.split(/\r?\n/);
  const descriptionIndex = lines.findIndex((line) => line.startsWith('Description:'));
  const description = descriptionIndex === -1 ? '' : lines[descriptionIndex].slice('Description:'.length).trim();
  let exceptionLine = '';
  for (let index = descriptionIndex + 1; descriptionIndex !== -1 && index < lines.length; index += 1) {
    if (lines[index].trim() !== '') {
      exceptionLine = lines[index].trim();
      break;
    }
  }
  const exception = EXCEPTION_CLASS.exec(exceptionLine)?.[1] ?? 'unknown';
  const kind = description === 'Watching Server' ? 'watchdog' : 'crash';
  return {
    id: shortId(kind, name),
    kind,
    process: exception.slice(0, PROCESS_MAX),
    sample: redactor.sample(`${description} | ${exceptionLine}`),
  };
}

/** A Java crash file (hs_err_pid<n>.log), which the JVM leaves instead of a crash report. */
export function classifyJavaCrash(name, text, redactor) {
  const header = text.split(/\r?\n/).filter((line) => line.startsWith('#')).slice(0, 20);
  let source = 'unknown';
  for (const line of header) {
    const signal = /^#\s+(SIG[A-Z]+)\b/.exec(line);
    if (signal) {
      source = signal[1];
      break;
    }
    if (/insufficient memory/i.test(line)) {
      source = 'insufficient-memory';
      break;
    }
    if (/Out of Memory Error/i.test(line)) {
      source = 'out-of-memory';
      break;
    }
    if (/Internal Error/i.test(line)) {
      source = 'internal-error';
      break;
    }
  }
  const summary = header.find((line) => /\S/.test(line.slice(1)) && !/^#\s*$/.test(line)) ?? '';
  return { id: shortId('java-crash', name), kind: 'java-crash', process: source, sample: redactor.sample(summary) };
}

const REQUESTED_STOP_JOBS = new Set(['stop', 'restart', 'try-restart', 'reload-or-restart', 'try-reload-or-restart']);

function token(value) {
  return String(value ?? '').replace(/[^A-Za-z0-9_-]/g, '').slice(0, 40);
}

/**
 * How the process ended, from one exit record, or null when it's a normal stop: a `systemctl` stop or restart (a
 * stop job was running), or the server exiting 0 on its own. The out-of-memory killer always counts.
 * `record` holds systemd's SERVICE_RESULT, EXIT_CODE and EXIT_STATUS, and the unit's running job type.
 */
export function exitSource(record) {
  const result = token(record.result);
  const code = token(record.code);
  const status = token(record.status);
  if (result === 'oom-kill') return 'oom-kill';
  if (REQUESTED_STOP_JOBS.has(token(record.job))) return null;
  if (code === 'exited') return status === '0' ? null : `exit-status-${status || 'unknown'}`;
  if (code === 'killed') return `signal-${status || 'unknown'}`;
  if (code === 'dumped') return `core-dump-${status || 'unknown'}`;
  if (result === 'success') return null;
  return result || 'unknown';
}

export function classifyExit(record) {
  const process = exitSource(record);
  if (process === null) return null;
  const time = Number(record.time) || 0;
  return {
    id: shortId('exit', String(time), process),
    kind: 'exit',
    process,
    sample: `result=${token(record.result)} code=${token(record.code)} status=${token(record.status)} job=${token(record.job) || 'none'}`,
    time,
  };
}
