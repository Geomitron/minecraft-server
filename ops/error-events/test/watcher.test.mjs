// Runs scans over a made-up server directory. Player names are invented; addresses are documentation ranges.

import assert from 'node:assert/strict';
import { appendFileSync, mkdirSync, mkdtempSync, renameSync, rmSync, utimesSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { afterEach, beforeEach, test } from 'node:test';
import { gzipSync } from 'node:zlib';
import { runCli } from '../src/cli.mjs';
import { renderMetrics } from '../src/metrics.mjs';
import { emptyState, loadState, saveState } from '../src/state.mjs';
import { scan } from '../src/watcher.mjs';

let root;
let config;
let now;

beforeEach(() => {
  root = mkdtempSync(join(tmpdir(), 'error-events-test-'));
  config = {
    minecraftDir: join(root, 'minecraft'),
    exitsFile: join(root, 'exits', 'exits.jsonl'),
    dataDir: join(root, 'data'),
  };
  mkdirSync(join(config.minecraftDir, 'logs'), { recursive: true });
  mkdirSync(join(config.minecraftDir, 'crash-reports'));
  mkdirSync(join(root, 'exits'));
  writeFileSync(join(config.minecraftDir, 'whitelist.json'), JSON.stringify([{ uuid: 'x', name: 'FixtureAlex' }]));
  now = Date.UTC(2026, 0, 2, 12, 0, 0);
});

afterEach(() => rmSync(root, { recursive: true, force: true }));

const latest = () => join(config.minecraftDir, 'logs', 'latest.log');
const line = (thread, level, message) => `[12:00:00] [${thread}/${level}]: ${message}\n`;
const WORLDGEN = line('Worker-Main-2', 'ERROR', 'Detected setBlock in a far chunk [3, -4]');
const AUTH = line('User Authenticator #1', 'ERROR', "Couldn't verify username because servers are unavailable");
const SCANNER = line('Netty Epoll Server IO #1', 'ERROR', 'Error sending packet clientbound/minecraft:disconnect');

function step(state, seconds = 15) {
  now += seconds * 1000;
  scan(state, config, now);
  return state;
}

/** Writes a file whose modification time is well before the current scan, as the watcher reads only settled files. */
function writeSettled(path, text) {
  writeFileSync(path, text);
  const past = new Date(now - 60_000);
  utimesSync(path, past, past);
}

function events(state) {
  return Object.values(state.events);
}

function newIds(state) {
  return events(state)
    .filter((event) => event.newAt !== null && event.handledAt === null)
    .map((event) => event.id);
}

test('existing errors are read once as known at first start; a new one afterwards is new', () => {
  writeFileSync(join(config.minecraftDir, 'logs', '2026-01-01-1.log.gz'), gzipSync(WORLDGEN + SCANNER));
  writeFileSync(latest(), AUTH + line('Server thread', 'INFO', 'Done'));
  writeSettled(join(config.minecraftDir, 'crash-reports', 'crash-2025-12-30_01.02.03-server.txt'), 'Description: Exception in server tick loop\n\njava.lang.RuntimeException: old\n');
  writeSettled(join(config.minecraftDir, 'hs_err_pid17.log'), '#  SIGSEGV (0xb) at pc=0x0\n');
  const state = step(emptyState());
  assert.equal(events(state).length, 4);
  assert.deepEqual(newIds(state), []);
  assert.equal(state.ignored['scanner-disconnect'], 1);
  assert.doesNotMatch(renderMetrics(state), /minecraft_error_event_new_timestamp_seconds\{/);

  appendFileSync(latest(), WORLDGEN + line('Server thread', 'ERROR', 'Failed to save chunk for FixtureAlex') + line('Server thread', 'INFO', 'Saved'));
  step(state);
  const fresh = events(state).filter((event) => event.newAt !== null);
  assert.equal(fresh.length, 1);
  assert.equal(fresh[0].sample, 'Failed to save chunk for <player>');
  const worldgen = events(state).find((event) => event.process === 'Worker-Main-N');
  assert.equal(worldgen.count, 2);
  assert.equal(worldgen.newAt, null);
  const metrics = renderMetrics(state);
  assert.match(metrics, new RegExp(`minecraft_error_event_new_timestamp_seconds\\{error_id="${fresh[0].id}",process="Server thread",kind="log-error"\\} ${now / 1000}`));
  assert.match(metrics, new RegExp(`minecraft_error_occurrences_total\\{error_id="${worldgen.id}",process="Worker-Main-N",kind="log-error"\\} 2`));
  assert.match(metrics, /minecraft_error_ignored_lines_total\{reason="scanner-disconnect"\} 1/);
  assert.match(metrics, new RegExp(`minecraft_error_watcher_last_scan_timestamp_seconds ${now / 1000}`));
});

test('an error that is last in the log waits one scan for its stack trace', () => {
  writeFileSync(latest(), line('Server thread', 'INFO', 'Done'));
  const state = step(emptyState());
  appendFileSync(latest(), line('Server thread', 'ERROR', 'Failed to tick entity'));
  step(state);
  assert.equal(events(state).length, 0);
  appendFileSync(latest(), 'java.lang.NullPointerException: x\n\tat a.B.c(B.java:1)\n');
  step(state);
  assert.equal(events(state).length, 1);
  assert.equal(events(state)[0].sample, 'Failed to tick entity | java.lang.NullPointerException');
  step(state);
  assert.equal(events(state)[0].count, 1);
});

test('lines written just before a rotation are read once, from the rotated file', () => {
  writeFileSync(latest(), line('Server thread', 'INFO', 'Starting'));
  const state = step(emptyState());
  appendFileSync(latest(), AUTH);
  step(state);
  const tail = line('Server thread', 'ERROR', 'Exception stopping the server');
  appendFileSync(latest(), tail);
  const oldText = line('Server thread', 'INFO', 'Starting') + AUTH + tail;
  renameSync(latest(), join(root, 'rotated.tmp'));
  writeFileSync(join(config.minecraftDir, 'logs', '2026-01-02-1.log.gz'), gzipSync(oldText));
  rmSync(join(root, 'rotated.tmp'));
  writeFileSync(latest(), line('Server thread', 'INFO', 'Starting again') + AUTH);
  step(state);
  step(state);
  const counts = Object.fromEntries(events(state).map((event) => [event.sample, event.count]));
  assert.deepEqual(counts, {
    "Couldn't verify username because servers are unavailable": 2,
    'Exception stopping the server': 1,
  });
});

test('new crash reports, watchdog reports and Java crash files are new events', () => {
  writeFileSync(latest(), '');
  const state = step(emptyState());
  writeSettled(join(config.minecraftDir, 'crash-reports', 'crash-2026-01-02_12.00.00-server.txt'), 'Description: Watching Server\n\njava.lang.Error: Watchdog\n');
  writeSettled(join(config.minecraftDir, 'crash-reports', 'crash-2026-01-02_12.00.01-server.txt'), 'Description: Exception in server tick loop\n\njava.lang.IllegalStateException: x\n');
  writeSettled(join(config.minecraftDir, 'hs_err_pid99.log'), '#\n# There is insufficient memory for the Java Runtime Environment to continue.\n');
  step(state);
  const kinds = events(state).map((event) => `${event.kind}:${event.process}`).sort();
  assert.deepEqual(kinds, ['crash:java.lang.IllegalStateException', 'java-crash:insufficient-memory', 'watchdog:java.lang.Error']);
  assert.equal(newIds(state).length, 3);
});

test('a crash file still being written is read on a later scan', () => {
  writeFileSync(latest(), '');
  const state = step(emptyState());
  writeFileSync(join(config.minecraftDir, 'crash-reports', 'crash-2026-01-02_12.00.02-server.txt'), 'Description: Exception in server tick loop\n\njava.lang.IllegalStateException: x\n');
  utimesSync(join(config.minecraftDir, 'crash-reports', 'crash-2026-01-02_12.00.02-server.txt'), new Date(now + 15_000), new Date(now + 15_000));
  step(state);
  assert.equal(events(state).length, 0);
  step(state);
  assert.equal(events(state).length, 1);
});

test('exit records: systemctl stops are ignored, other ends alert once each', () => {
  writeFileSync(latest(), '');
  const state = step(emptyState());
  const exits = [
    { time: 1767355200, result: 'success', code: 'exited', status: '143', job: 'stop' },
    { time: 1767355300, result: 'oom-kill', code: 'killed', status: 'KILL', job: '' },
    { time: 1767355400, result: 'exit-code', code: 'exited', status: '1', job: '' },
  ];
  writeFileSync(config.exitsFile, `${exits.map((exit) => JSON.stringify(exit)).join('\n')}\n{"time":176735`);
  step(state);
  assert.deepEqual(events(state).map((event) => event.process).sort(), ['exit-status-1', 'oom-kill']);
  appendFileSync(config.exitsFile, '5500,"result":"signal","code":"killed","status":"KILL","job":""}\n');
  step(state);
  step(state);
  assert.deepEqual(events(state).map((event) => event.process).sort(), ['exit-status-1', 'oom-kill', 'signal-KILL']);
  assert.equal(newIds(state).length, 3);
});

test('the handled command clears an alert; the error coming back makes it new again', () => {
  writeFileSync(latest(), '');
  const state = step(emptyState());
  appendFileSync(latest(), AUTH + line('Server thread', 'INFO', 'tick'));
  step(state);
  const [id] = newIds(state);
  saveState(config.dataDir, state);

  const output = [];
  assert.equal(runCli(['show', id], { config, write: (text) => output.push(text) }), 0);
  assert.ok(output.includes(`sample:      Couldn't verify username because servers are unavailable`));
  assert.equal(runCli(['handled', 'ffffffff'], { config, write: () => {} }), 1);
  assert.equal(runCli(['handled', id], { config, write: () => {} }), 0);

  step(state);
  assert.deepEqual(newIds(state), []);
  assert.equal(state.events[id].handledAt, now);
  assert.doesNotMatch(renderMetrics(state), /minecraft_error_event_new_timestamp_seconds\{/);

  appendFileSync(latest(), AUTH + line('Server thread', 'INFO', 'tick'));
  step(state);
  assert.deepEqual(newIds(state), [id]);
  assert.equal(state.events[id].newAt, now);
  assert.equal(state.events[id].count, 2);

  saveState(config.dataDir, state);
  assert.deepEqual(loadState(config.dataDir), state);
  const listed = [];
  runCli(['list', '--open'], { config, write: (text) => listed.push(text) });
  assert.equal(listed.length, 1);
  assert.match(listed[0], new RegExp(`^${id}  new`));
});

test('label values are escaped', () => {
  const state = emptyState();
  state.events.abc = { id: 'abc', kind: 'log-error', process: 'odd "thread"\\x', count: 1, newAt: 1000, handledAt: null };
  assert.match(renderMetrics(state), /process="odd \\"thread\\"\\\\x"/);
});
