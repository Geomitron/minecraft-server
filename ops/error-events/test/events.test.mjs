// Made-up fixtures only: player names are invented and addresses come from the documentation ranges.

import assert from 'node:assert/strict';
import { test } from 'node:test';
import {
  classifyCrashReport,
  classifyExit,
  classifyJavaCrash,
  classifyLogRecord,
  makeRedactor,
  splitLogRecords,
} from '../src/events.mjs';

const redactor = makeRedactor(['FixtureAlex', 'BuilderBee']);

function record(text) {
  const { records } = splitLogRecords(Buffer.from(text));
  assert.equal(records.length, 1);
  return records[0];
}

test('the same error from different players, addresses and numbers gets one id', () => {
  const first = classifyLogRecord(
    record('[10:00:01] [Server thread/ERROR]: Failed to handle packet for FixtureAlex (/203.0.113.7:51234) at 12, 64, -30\n'),
    redactor,
  );
  const second = classifyLogRecord(
    record('[11:20:44] [Server thread/ERROR]: Failed to handle packet for BuilderBee (/198.51.100.23:40001) at 8, 70, 5\n'),
    redactor,
  );
  assert.equal(first.id, second.id);
  assert.match(first.id, /^[0-9a-f]{8}$/);
  assert.equal(first.kind, 'log-error');
  assert.equal(first.process, 'Server thread');
  assert.equal(first.sample, 'Failed to handle packet for <player> (/<address>) at 12, 64, -30');
});

test('names, profile ids and IPv6 addresses are blanked in the sample', () => {
  const result = classifyLogRecord(
    record(
      "[10:00:01] [Server thread/ERROR]: Lost GameProfile[id=0f1e2d3c-4b5a-6978-8796-a5b4c3d2e1f0,name=SomeStranger] from [2001:db8::17]:25565 for player 'Unknown_Kid'\n",
    ),
    redactor,
  );
  assert.equal(result.sample, "Lost GameProfile[id=<uuid>,name=<player>] from <address> for player '<player>'");
});

test('numbered pool threads are one source, and the first stack line splits errors by exception', () => {
  const plain = classifyLogRecord(record('[10:00:01] [Worker-Main-3/ERROR]: Detected setBlock in a far chunk [4, -9]\n'), redactor);
  const other = classifyLogRecord(record('[10:00:01] [Worker-Main-11/ERROR]: Detected setBlock in a far chunk [2, 7]\n'), redactor);
  assert.equal(plain.process, 'Worker-Main-N');
  assert.equal(plain.id, other.id);

  const withNpe = classifyLogRecord(
    record('[10:00:01] [Server thread/ERROR]: Failed to create block entity\njava.lang.NullPointerException: oops\n\tat a.b.C.d(C.java:1)\n'),
    redactor,
  );
  const withIse = classifyLogRecord(
    record('[10:00:01] [Server thread/ERROR]: Failed to create block entity\njava.lang.IllegalStateException: no\n\tat a.b.C.d(C.java:1)\n'),
    redactor,
  );
  assert.notEqual(withNpe.id, withIse.id);
  assert.equal(withNpe.sample, 'Failed to create block entity | java.lang.NullPointerException');
});

test('FATAL lines count; WARN and INFO lines do not', () => {
  assert.equal(classifyLogRecord(record('[10:00:01] [main/FATAL]: Failed to start the minecraft server\n'), redactor).kind, 'log-error');
  assert.equal(classifyLogRecord(record("[10:00:01] [Server thread/WARN]: Can't keep up! Is the server overloaded?\n"), redactor), null);
  assert.equal(classifyLogRecord(record('[10:00:01] [Server thread/INFO]: Done (3.2s)!\n'), redactor), null);
});

test('scanner disconnect-packet errors and invalid-session rejections are ignored', () => {
  const scanner = classifyLogRecord(
    record(
      "[10:00:01] [Netty Epoll Server IO #2/ERROR]: Error sending packet clientbound/minecraft:disconnect\nio.netty.handler.codec.EncoderException: Sending unknown packet 'x'\n",
    ),
    redactor,
  );
  const session = classifyLogRecord(
    record("[10:00:01] [User Authenticator #4/ERROR]: Username 'SomeStranger' tried to join with an invalid session\n"),
    redactor,
  );
  assert.deepEqual(scanner, { ignored: 'scanner-disconnect' });
  assert.deepEqual(session, { ignored: 'invalid-session' });
});

test('only complete lines are split into records', () => {
  const { records, consumed } = splitLogRecords(Buffer.from('[10:00:01] [main/INFO]: one\n[10:00:02] [main/ERROR]: tw'));
  assert.equal(records.length, 1);
  assert.equal(consumed, 28);
});

const CRASH = `---- Minecraft Crash Report ----
// Made-up fixture.

Time: 2026-01-02 03:04:05
Description: Exception in server tick loop

java.lang.IllegalStateException: Made-up failure near FixtureAlex
\tat net.example.Fixture.tick(Fixture.java:10)
`;

const WATCHDOG = `---- Minecraft Crash Report ----
// Made-up fixture.

Time: 2026-01-02 03:04:05
Description: Watching Server

java.lang.Error: Watchdog
\tat net.example.Fixture.tick(Fixture.java:10)
`;

test('a crash report is a crash with its exception class as the source; the watchdog is its own kind', () => {
  const crash = classifyCrashReport('crash-2026-01-02_03.04.05-server.txt', CRASH, redactor);
  assert.equal(crash.kind, 'crash');
  assert.equal(crash.process, 'java.lang.IllegalStateException');
  assert.equal(crash.sample, 'Exception in server tick loop | java.lang.IllegalStateException: Made-up failure near <player>');
  const watchdog = classifyCrashReport('crash-2026-01-02_04.00.00-server.txt', WATCHDOG, redactor);
  assert.equal(watchdog.kind, 'watchdog');
  assert.equal(watchdog.process, 'java.lang.Error');
  assert.notEqual(crash.id, watchdog.id);
});

test('a Java crash file is a java-crash with the signal or failure as the source', () => {
  const segv = classifyJavaCrash(
    'hs_err_pid4242.log',
    '#\n# A fatal error has been detected by the Java Runtime Environment:\n#\n#  SIGSEGV (0xb) at pc=0x0000, pid=4242, tid=4243\n#\n',
    redactor,
  );
  assert.deepEqual([segv.kind, segv.process], ['java-crash', 'SIGSEGV']);
  const memory = classifyJavaCrash(
    'hs_err_pid4343.log',
    '#\n# There is insufficient memory for the Java Runtime Environment to continue.\n',
    redactor,
  );
  assert.equal(memory.process, 'insufficient-memory');
});

test('exits: systemctl stops and clean exits are normal; failures, signals and the OOM killer alert', () => {
  const at = { time: 1767000000 };
  assert.equal(classifyExit({ ...at, result: 'success', code: 'exited', status: '143', job: 'stop' }), null);
  assert.equal(classifyExit({ ...at, result: 'success', code: 'exited', status: '143', job: 'restart' }), null);
  assert.equal(classifyExit({ ...at, result: 'success', code: 'exited', status: '0', job: '' }), null);
  assert.equal(classifyExit({ ...at, result: 'exit-code', code: 'exited', status: '1', job: '' }).process, 'exit-status-1');
  assert.equal(classifyExit({ ...at, result: 'success', code: 'exited', status: '143', job: '' }).process, 'exit-status-143');
  assert.equal(classifyExit({ ...at, result: 'signal', code: 'killed', status: 'KILL', job: '' }).process, 'signal-KILL');
  assert.equal(classifyExit({ ...at, result: 'core-dump', code: 'dumped', status: 'ABRT', job: '' }).process, 'core-dump-ABRT');
  assert.equal(classifyExit({ ...at, result: 'oom-kill', code: 'killed', status: 'KILL', job: 'stop' }).process, 'oom-kill');
  const exit = classifyExit({ ...at, result: 'oom-kill', code: 'killed', status: 'KILL', job: '' });
  assert.equal(exit.kind, 'exit');
  assert.notEqual(exit.id, classifyExit({ time: 1767000500, result: 'oom-kill', code: 'killed', status: 'KILL' }).id);
});
