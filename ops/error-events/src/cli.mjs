// Reads the watcher's events and marks them handled. Run inside the container:
//   docker compose exec watcher node src/cli.mjs list [--open]
//   docker compose exec watcher node src/cli.mjs show <id>
//   docker compose exec watcher node src/cli.mjs handled <id...>
// A handled mark is a request file the watcher applies on its next scan, so the watcher stays the state's only
// writer.

import { writeFileSync, mkdirSync } from 'node:fs';
import { join } from 'node:path';
import { pathToFileURL } from 'node:url';
import { loadConfig } from './config.mjs';
import { loadState } from './state.mjs';
import { handledDir } from './watcher.mjs';

export const USAGE = `Usage:
  node src/cli.mjs list [--open]     every event, newest first; --open: only new events not yet handled
  node src/cli.mjs show <id>         one event: kind, source, counts, times, sample line and where it was read
  node src/cli.mjs handled <id...>   mark events handled; one that comes back is new again`;

function iso(milliseconds) {
  return milliseconds === null ? '-' : new Date(milliseconds).toISOString();
}

function status(event) {
  if (event.handledAt !== null) return 'handled';
  return event.newAt === null ? 'known' : 'new';
}

export function runCli(args, { config, write }) {
  const [command, ...rest] = args;
  const state = loadState(config.dataDir);
  if (command === 'list' && (rest.length === 0 || (rest.length === 1 && rest[0] === '--open'))) {
    const events = Object.values(state.events)
      .filter((event) => rest[0] !== '--open' || status(event) === 'new')
      .sort((a, b) => b.lastSeenAt - a.lastSeenAt);
    for (const event of events) {
      write(`${event.id}  ${status(event).padEnd(7)}  ${event.kind.padEnd(10)}  ${String(event.count).padStart(6)}  ${iso(event.lastSeenAt)}  ${event.process}`);
    }
    return 0;
  }
  if (command === 'show' && rest.length === 1) {
    const event = state.events[rest[0]];
    if (!event) {
      write(`No event ${rest[0]}.`);
      return 1;
    }
    write(`id:          ${event.id}`);
    write(`status:      ${status(event)}`);
    write(`kind:        ${event.kind}`);
    write(`source:      ${event.process}`);
    write(`count:       ${event.count}`);
    write(`first seen:  ${iso(event.firstSeenAt)}`);
    write(`last seen:   ${iso(event.lastSeenAt)}`);
    write(`new since:   ${iso(event.newAt)}`);
    write(`handled:     ${iso(event.handledAt)}`);
    write(`read from:   ${event.location}`);
    write(`sample:      ${event.sample}`);
    return 0;
  }
  if (command === 'handled' && rest.length > 0) {
    const unknown = rest.filter((id) => !state.events[id]);
    if (unknown.length > 0) {
      write(`No event ${unknown.join(', ')}; nothing marked.`);
      return 1;
    }
    mkdirSync(handledDir(config.dataDir), { recursive: true, mode: 0o700 });
    for (const id of rest) writeFileSync(join(handledDir(config.dataDir), `${id}.${Date.now()}`), '', { mode: 0o600 });
    write(`Marked ${rest.join(', ')} handled; the watcher applies it on its next scan.`);
    return 0;
  }
  write(USAGE);
  return 2;
}

if (import.meta.url === pathToFileURL(process.argv[1]).href) {
  process.exitCode = runCli(process.argv.slice(2), { config: loadConfig(), write: (line) => console.log(line) });
}
