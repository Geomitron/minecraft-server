// Paths and timings, from the environment, with the defaults the Compose project uses.

export function loadConfig(env = process.env) {
  return {
    minecraftDir: env.MINECRAFT_DIR ?? '/minecraft',
    exitsFile: env.EXITS_FILE ?? '/exits/exits.jsonl',
    dataDir: env.DATA_DIR ?? '/app/data',
    port: Number(env.METRICS_PORT ?? 9464),
    scanIntervalMs: Number(env.SCAN_INTERVAL_SECONDS ?? 15) * 1000,
  };
}
