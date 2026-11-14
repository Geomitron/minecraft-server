// One scan of the server directory: applies handled marks, then reads new log lines, rotated logs, crash reports,
// Java crash files and exit records into the state. The first scan reads everything already there as known.

import { createHash } from 'node:crypto';
import { closeSync, fstatSync, mkdirSync, openSync, readFileSync, readSync, readdirSync, statSync, unlinkSync } from 'node:fs';
import { join } from 'node:path';
import { gunzipSync } from 'node:zlib';
import {
  classifyCrashReport,
  classifyExit,
  classifyJavaCrash,
  classifyLogRecord,
  makeRedactor,
  splitLogRecords,
} from './events.mjs';

const PREFIX_MAX = 4096;
/** Crash and Java crash files younger than this may still be being written; the next scan reads them. */
const SETTLE_MS = 5000;
const PLAYER_FILES = ['whitelist.json', 'ops.json', 'usercache.json', 'banned-players.json'];

export function handledDir(dataDir) {
  return join(dataDir, 'handled');
}

function readPlayerNames(minecraftDir) {
  const names = [];
  for (const file of PLAYER_FILES) {
    try {
      const entries = JSON.parse(readFileSync(join(minecraftDir, file), 'utf8'));
      if (Array.isArray(entries)) for (const entry of entries) if (entry?.name) names.push(entry.name);
    } catch {
      // A missing or half-written list only means fewer known names this scan.
    }
  }
  return names;
}

function listDir(path) {
  try {
    return readdirSync(path);
  } catch (error) {
    if (error.code === 'ENOENT') return [];
    throw error;
  }
}

function hash(buffer) {
  return createHash('sha256').update(buffer).digest('hex');
}

function readRange(path, start, end) {
  const descriptor = openSync(path, 'r');
  try {
    const buffer = Buffer.alloc(Math.max(0, end - start));
    let read = 0;
    while (read < buffer.length) {
      const count = readSync(descriptor, buffer, read, buffer.length - read, start + read);
      if (count === 0) break;
      read += count;
    }
    return buffer.subarray(0, read);
  } finally {
    closeSync(descriptor);
  }
}

/**
 * Records one occurrence. A live occurrence of an unseen event, or of one marked handled, makes it new; during the
 * first scan every occurrence is known.
 */
function observe(state, observation, { now, live, location }) {
  const existing = state.events[observation.id];
  if (!existing) {
    state.events[observation.id] = {
      id: observation.id,
      kind: observation.kind,
      process: observation.process,
      sample: observation.sample,
      location,
      count: 1,
      firstSeenAt: now,
      lastSeenAt: now,
      newAt: live ? now : null,
      handledAt: null,
    };
    return;
  }
  existing.count += 1;
  existing.lastSeenAt = now;
  existing.sample = observation.sample;
  existing.location = location;
  if (existing.handledAt !== null && live) {
    existing.handledAt = null;
    existing.newAt = now;
  }
}

function processLogRecords(state, records, context) {
  for (const record of records) {
    const result = classifyLogRecord(record, context.redactor);
    if (result === null) continue;
    if (result.ignored) {
      state.ignored[result.ignored] = (state.ignored[result.ignored] ?? 0) + 1;
      continue;
    }
    observe(state, result, context);
  }
}

function applyHandled(state, dataDir, now) {
  const directory = handledDir(dataDir);
  for (const name of listDir(directory)) {
    const id = name.split('.')[0];
    const event = state.events[id];
    if (event) {
      event.handledAt = now;
      event.newAt = null;
    }
    unlinkSync(join(directory, name));
  }
}

function scanRotatedLogs(state, logsDir, context) {
  const seen = new Set(state.rotatedLogs);
  const fresh = listDir(logsDir)
    .filter((name) => name.endsWith('.log.gz') && !seen.has(name))
    .sort();
  for (const name of fresh) {
    const content = gunzipSync(readFileSync(join(logsDir, name)));
    const latest = state.latestLog;
    // A rotated log that starts with what was read from latest.log continues from there.
    const continues =
      latest.prefixHash !== null &&
      content.length >= latest.offset &&
      hash(content.subarray(0, latest.prefixLength)) === latest.prefixHash;
    const from = continues ? latest.offset : 0;
    processLogRecords(state, splitLogRecords(content.subarray(from)).records, { ...context, location: `logs/${name}` });
    // Its tail is read now, so the next latest.log starts from the beginning, even if it reuses the inode.
    if (continues) state.latestLog = { ino: null, offset: 0, heldAt: null, prefixLength: 0, prefixHash: null };
    state.rotatedLogs.push(name);
  }
}

function scanLatestLog(state, logsDir, context) {
  const path = join(logsDir, 'latest.log');
  let descriptorStat;
  try {
    const descriptor = openSync(path, 'r');
    try {
      descriptorStat = fstatSync(descriptor);
    } finally {
      closeSync(descriptor);
    }
  } catch (error) {
    if (error.code === 'ENOENT') return;
    throw error;
  }
  const latest = state.latestLog;
  const ino = String(descriptorStat.ino);
  if (latest.ino !== ino || descriptorStat.size < latest.offset) {
    // A new latest.log. If the old one was rotated, its unread tail was read from the rotated file.
    state.latestLog = { ino, offset: 0, heldAt: null, prefixLength: 0, prefixHash: null };
  }
  const current = state.latestLog;
  const chunk = readRange(path, current.offset, descriptorStat.size);
  const { records, consumed } = splitLogRecords(chunk);
  let commit = records;
  let next = current.offset + consumed;
  let heldAt = null;
  if (context.live && records.length > 0) {
    // The last record may still be getting its stack trace. Hold it until it's last in two scans in a row.
    const lastAt = current.offset + records[records.length - 1].start;
    if (current.heldAt !== lastAt) {
      commit = records.slice(0, -1);
      next = lastAt;
      heldAt = lastAt;
    }
  }
  processLogRecords(state, commit, { ...context, location: 'logs/latest.log' });
  current.offset = next;
  current.heldAt = heldAt;
  const prefixLength = Math.min(PREFIX_MAX, current.offset);
  if (prefixLength !== current.prefixLength) {
    current.prefixLength = prefixLength;
    current.prefixHash = hash(readRange(path, 0, prefixLength));
  }
}

function settledFiles(directory, names, seen, now) {
  return names
    .filter((name) => !seen.has(name))
    .filter((name) => statSync(join(directory, name)).mtimeMs <= now - SETTLE_MS)
    .sort();
}

function scanCrashReports(state, minecraftDir, context) {
  const directory = join(minecraftDir, 'crash-reports');
  const names = listDir(directory).filter((name) => name.endsWith('.txt'));
  for (const name of settledFiles(directory, names, new Set(state.crashReports), context.now)) {
    const observation = classifyCrashReport(name, readFileSync(join(directory, name), 'utf8'), context.redactor);
    observe(state, observation, { ...context, location: `crash-reports/${name}` });
    state.crashReports.push(name);
  }
}

function scanJavaCrashes(state, minecraftDir, context) {
  const names = listDir(minecraftDir).filter((name) => /^hs_err_pid\d+\.log$/.test(name));
  for (const name of settledFiles(minecraftDir, names, new Set(state.javaCrashes), context.now)) {
    const observation = classifyJavaCrash(name, readFileSync(join(minecraftDir, name), 'utf8'), context.redactor);
    observe(state, observation, { ...context, location: name });
    state.javaCrashes.push(name);
  }
}

function scanExits(state, exitsFile, context) {
  let size;
  try {
    size = statSync(exitsFile).size;
  } catch (error) {
    if (error.code === 'ENOENT') return;
    throw error;
  }
  if (size < state.exitsOffset) state.exitsOffset = 0;
  const chunk = readRange(exitsFile, state.exitsOffset, size);
  const end = chunk.lastIndexOf(0x0a) + 1;
  for (const line of chunk.toString('utf8', 0, end).split('\n')) {
    if (line.trim() === '') continue;
    let record;
    try {
      record = JSON.parse(line);
    } catch {
      record = { result: 'unreadable-record' };
    }
    const observation = classifyExit(record);
    if (observation) observe(state, observation, { ...context, location: 'exits' });
  }
  state.exitsOffset += end;
}

/** Runs one scan, mutating `state`. `now` is Unix milliseconds. */
export function scan(state, config, now) {
  const live = state.initializedAt !== null;
  mkdirSync(handledDir(config.dataDir), { recursive: true, mode: 0o700 });
  applyHandled(state, config.dataDir, now);
  const context = { now, live, redactor: makeRedactor(readPlayerNames(config.minecraftDir)) };
  const logsDir = join(config.minecraftDir, 'logs');
  scanRotatedLogs(state, logsDir, context);
  scanLatestLog(state, logsDir, context);
  scanCrashReports(state, config.minecraftDir, context);
  scanJavaCrashes(state, config.minecraftDir, context);
  scanExits(state, config.exitsFile, context);
  if (!live) state.initializedAt = now;
  state.lastScanAt = now;
}
