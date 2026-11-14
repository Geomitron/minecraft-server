// The watcher: scans on a fixed interval and serves the metrics on GET /metrics.
// A scan that fails leaves the last-scan time stale, which is what the watcher-down alert reads.

import { createServer } from 'node:http';
import { loadConfig } from './config.mjs';
import { renderMetrics } from './metrics.mjs';
import { loadState, saveState } from './state.mjs';
import { scan } from './watcher.mjs';

const config = loadConfig();
const state = loadState(config.dataDir);

function runScan() {
  try {
    scan(state, config, Date.now());
    saveState(config.dataDir, state);
  } catch (error) {
    // The error's code and message name a file or a parse failure, never log content.
    console.error(`Scan failed: ${error.code ?? error.name}: ${error.message}`);
  }
}

runScan();
const timer = setInterval(runScan, config.scanIntervalMs);

const server = createServer((request, response) => {
  if (request.method === 'GET' && request.url === '/metrics') {
    response.writeHead(200, { 'Content-Type': 'text/plain; version=0.0.4; charset=utf-8' });
    response.end(renderMetrics(state));
    return;
  }
  response.writeHead(404).end();
});
server.listen(config.port, () => console.log(`Serving metrics on port ${config.port}`));

function shutdown() {
  clearInterval(timer);
  server.close(() => process.exit(0));
}
process.on('SIGTERM', shutdown);
process.on('SIGINT', shutdown);
