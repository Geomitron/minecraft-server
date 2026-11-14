// The Prometheus text exposition of the state. The README's "Metrics contract" section is the authority on what
// these mean; labels carry only an event's id, kind and source, never its text.

import { IGNORED_REASONS } from './events.mjs';

function escapeLabel(value) {
  return String(value).replace(/\\/g, '\\\\').replace(/"/g, '\\"').replace(/\n/g, '\\n');
}

function eventLabels(event) {
  return `error_id="${escapeLabel(event.id)}",process="${escapeLabel(event.process)}",kind="${escapeLabel(event.kind)}"`;
}

function seconds(milliseconds) {
  return String(milliseconds / 1000);
}

export function renderMetrics(state) {
  const events = Object.values(state.events).sort((a, b) => a.id.localeCompare(b.id));
  const lines = [
    '# HELP minecraft_error_event_new_timestamp_seconds Unix time an unhandled event became new: first seen, or seen again after it was marked handled.',
    '# TYPE minecraft_error_event_new_timestamp_seconds gauge',
  ];
  for (const event of events) {
    if (event.newAt !== null && event.handledAt === null) {
      lines.push(`minecraft_error_event_new_timestamp_seconds{${eventLabels(event)}} ${seconds(event.newAt)}`);
    }
  }
  lines.push(
    '# HELP minecraft_error_occurrences_total Occurrences of each event, including those read as known at first start.',
    '# TYPE minecraft_error_occurrences_total counter',
  );
  for (const event of events) {
    lines.push(`minecraft_error_occurrences_total{${eventLabels(event)}} ${event.count}`);
  }
  lines.push(
    '# HELP minecraft_error_ignored_lines_total Log ERROR records the watcher ignores, by reason.',
    '# TYPE minecraft_error_ignored_lines_total counter',
  );
  for (const reason of IGNORED_REASONS) {
    lines.push(`minecraft_error_ignored_lines_total{reason="${reason}"} ${state.ignored[reason] ?? 0}`);
  }
  lines.push(
    '# HELP minecraft_error_watcher_last_scan_timestamp_seconds Unix time of the last scan that completed.',
    '# TYPE minecraft_error_watcher_last_scan_timestamp_seconds gauge',
  );
  if (state.lastScanAt !== null) {
    lines.push(`minecraft_error_watcher_last_scan_timestamp_seconds ${seconds(state.lastScanAt)}`);
  }
  return `${lines.join('\n')}\n`;
}
