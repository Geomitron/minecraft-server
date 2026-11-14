#!/bin/sh
# Appends how the unit's main process ended to the exits file the watcher reads. Run by the drop-in's ExecStopPost
# with the unit's name as $1. systemd sets SERVICE_RESULT, EXIT_CODE and EXIT_STATUS; the unit's running job is
# "stop" or "restart" when the stop came through systemctl (or a shutdown), and empty when the process ended on its
# own.
unit="$1"
file="${MINECRAFT_EXITS_FILE:-/home/minecraft-error-events/exits/exits.jsonl}"
job=$(systemctl list-jobs --no-legend --full 2>/dev/null | awk -v unit="$unit" '$2 == unit { print $3; exit }')
printf '{"time":%s,"result":"%s","code":"%s","status":"%s","job":"%s"}\n' \
  "$(date +%s)" "$SERVICE_RESULT" "$EXIT_CODE" "$EXIT_STATUS" "$job" >>"$file"
