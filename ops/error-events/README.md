# Error watcher

A small Node service that runs beside the Minecraft server, reads what the server leaves behind, and exports counts
for Prometheus, so Grafana can alert when something goes wrong. It reads the server directory read-only and never
changes the server.

## What it watches

Every 15 seconds it scans the server directory (mounted at `/minecraft`) and records each distinct error as an
event with an 8-character id, a kind and a source:

| Kind         | Read from                                                     | Source (`process` label)                          |
| ------------ | ------------------------------------------------------------- | ------------------------------------------------- |
| `log-error`  | ERROR and FATAL lines in `logs/latest.log` and rotated logs    | the logging thread, with numbers as `N`           |
| `crash`      | each new file in `crash-reports/`                             | the exception's class name                        |
| `watchdog`   | a crash report whose description is `Watching Server`         | the exception's class name                        |
| `java-crash` | each new `hs_err_pid<n>.log` in the server directory           | the signal (`SIGSEGV`) or failure (`insufficient-memory`) |
| `exit`       | each exit record the systemd drop-in writes                   | how the process ended (below)                     |

A log error's id comes from its source, its message and the exception class on the line after it, with player names,
addresses, profile ids, numbers and object hashes blanked, so the same error from different players or places is one
event. Player names are the ones in `whitelist.json`, `ops.json`, `usercache.json` and `banned-players.json`, plus
any a game profile or a quoted username names. Each event keeps its count, when it was first and last seen, where it
was last read, and one sample line with names and addresses blanked. Crash reports, Java crash files and exits are
one event each.

It ignores two kinds of ERROR line that say nothing about the server, counting them instead:
`scanner-disconnect` (`Error sending packet clientbound/minecraft:disconnect`, from internet scanners) and
`invalid-session` (`Username '…' tried to join with an invalid session`). WARN and lower lines are ignored.

At first start it reads everything already there (all logs, crash reports and Java crash files) as known, so only
what happens afterwards is new. An event is new when it's first seen after that, or seen again after it was marked
handled.

An error line that's last in the log is read on the next scan, so its stack trace has arrived. Lines written just
before the server rotates its log are read from the rotated file.

### Exits

The drop-in [`error-events.conf`](error-events.conf) runs [`record-exit.sh`](record-exit.sh) each time the server
process ends. It appends one JSON line to `exits/exits.jsonl` with systemd's `SERVICE_RESULT`, `EXIT_CODE` and
`EXIT_STATUS`, and the unit's running job type. The watcher reads that file (mounted read-only at `/exits`) and makes
an event, whose source is how the process ended:

- `oom-kill`: the out-of-memory killer, always.
- Nothing, when a `stop` or `restart` job was running (`systemctl stop`, `systemctl restart`, a shutdown), or the
  server exited 0 on its own (a `/stop` in game or at the console).
- `exit-status-<n>`: a failing exit code. `exit-status-143` is a SIGTERM that didn't come through `systemctl`.
- `signal-<NAME>` or `core-dump-<NAME>`: a kill signal.
- Otherwise systemd's `SERVICE_RESULT`, such as `timeout`.

## Metrics contract

The watcher serves `GET /metrics` on port 9464 inside its container. It publishes no port: Prometheus reaches it on
the `minecraft-error-events` Docker network at **`minecraft-error-events:9464`**. Use the scrape job name
**`minecraft-error-events`**. Prometheus scrapes it at its global 15-second interval, which keeps an alert's email
within about two minutes of the error.

| Metric                                                | Type    | Labels                       | Meaning |
| ----------------------------------------------------- | ------- | ---------------------------- | ------- |
| `minecraft_error_event_new_timestamp_seconds`         | gauge   | `error_id`, `process`, `kind` | Unix time an unhandled event became new. Present only for new events not yet marked handled; marking one handled removes its series. |
| `minecraft_error_occurrences_total`                   | counter | `error_id`, `process`, `kind` | Occurrences of each event, including those read as known at first start. Kept across restarts. |
| `minecraft_error_ignored_lines_total`                 | counter | `reason` (`scanner-disconnect`, `invalid-session`) | Ignored ERROR lines. |
| `minecraft_error_watcher_last_scan_timestamp_seconds` | gauge   | none                          | Unix time of the last scan that completed. |

`error_id` is 8 lowercase hex characters, `kind` is one of the kinds above, and `process` is the source. Labels never
carry an error's text.

Rule expressions these support:

- New error: `time() - minecraft_error_event_new_timestamp_seconds < 900`.
- Rate jump, per event: over 30 in the last hour and over 3 times its hourly average across the 24 hours before,
  only once the event has a day of history:

  ```promql
  increase(minecraft_error_occurrences_total[1h]) > 30
    and increase(minecraft_error_occurrences_total[1h])
      > 3 * increase(minecraft_error_occurrences_total[24h] offset 1h) / 24
    and minecraft_error_occurrences_total offset 25h
  ```

- Watcher down, for 10 minutes: `up{job="minecraft-error-events"} == 0`, a missing `up` series, or
  `time() - minecraft_error_watcher_last_scan_timestamp_seconds > 120`.

## Reading and handling events

Run these from the deployed folder:

```sh
docker compose exec watcher node src/cli.mjs list           # every event, newest first
docker compose exec watcher node src/cli.mjs list --open    # new events not yet handled
docker compose exec watcher node src/cli.mjs show <id>      # kind, source, counts, times, where it was read, sample
docker compose exec watcher node src/cli.mjs handled <id>…  # mark fixed; it's new again if it comes back
```

`handled` takes effect on the watcher's next scan. `show` names the file an event was read from, so the full error
can be read there.

## Deploy

The server runs as `minecraft.service` from `/home/minecraft`. As root on the server:

1. Copy this folder, without `test/`, to `/home/minecraft-error-events`, then create its state folders, readable
   only by root:

   ```sh
   install -d -m 700 /home/minecraft-error-events/data /home/minecraft-error-events/exits
   ```

2. Create the network, internal so the watcher has no route out, and start the watcher:

   ```sh
   docker network create --internal minecraft-error-events
   cd /home/minecraft-error-events && docker compose up -d
   ```

3. Install the drop-in and reload systemd. This needs no server restart: the hook runs the next time the server
   stops.

   ```sh
   install -D -m 644 error-events.conf /etc/systemd/system/minecraft.service.d/error-events.conf
   systemctl daemon-reload
   systemctl show minecraft.service -p ExecStopPost
   ```

4. Add Prometheus to the `minecraft-error-events` network and the scrape job above.

To update, copy the changed files and run `docker compose up -d --force-recreate`. The state in `data/` survives.

## Tests

```sh
node --test 'test/*.test.mjs'
```

The tests use made-up fixtures only.
