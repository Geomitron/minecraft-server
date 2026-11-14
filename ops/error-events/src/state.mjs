// The watcher's state file: every event it has seen, how far it has read each source, and the ignored-line counts.
// Written atomically (temporary file, then rename) so a crash mid-write leaves the previous state.

import { mkdirSync, readFileSync, renameSync, writeFileSync } from 'node:fs';
import { join } from 'node:path';

const VERSION = 1;

export function emptyState() {
  return {
    version: VERSION,
    initializedAt: null,
    lastScanAt: null,
    latestLog: { ino: null, offset: 0, heldAt: null, prefixLength: 0, prefixHash: null },
    rotatedLogs: [],
    crashReports: [],
    javaCrashes: [],
    exitsOffset: 0,
    ignored: {},
    events: {},
  };
}

export function loadState(dataDir) {
  let text;
  try {
    text = readFileSync(join(dataDir, 'state.json'), 'utf8');
  } catch (error) {
    if (error.code === 'ENOENT') return emptyState();
    throw error;
  }
  const state = JSON.parse(text);
  if (state.version !== VERSION) throw new Error(`Unsupported state version ${state.version}`);
  return state;
}

export function saveState(dataDir, state) {
  mkdirSync(dataDir, { recursive: true, mode: 0o700 });
  const path = join(dataDir, 'state.json');
  const temporary = `${path}.tmp`;
  writeFileSync(temporary, `${JSON.stringify(state)}\n`, { mode: 0o600 });
  renameSync(temporary, path);
}
