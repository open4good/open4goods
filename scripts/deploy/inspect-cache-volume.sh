#!/usr/bin/env bash
# Report cache capacity and classes without disclosing cache-entry names or URLs.
set -euo pipefail

readonly cache_root='/opt/open4goods/.cached'
readonly diskb_mount='/diskb'

[[ -d "$cache_root" ]] || { echo 'cache root is unavailable' >&2; exit 1; }
findmnt --mountpoint "$diskb_mount" >/dev/null || { echo 'DiskB is not mounted at /diskb' >&2; exit 1; }

printf 'filesystems_bytes\n'
df -B1 --output=target,size,used,avail,pcent / "$diskb_mount"
printf 'cache_total_bytes\n'
du -sxB1 "$cache_root"
sum_selected() {
  find "$@" -print0 | du -sB1 --files0-from=- -c 2>/dev/null | awk 'END { print $1 + 0 }'
}

flat_bytes="$(sum_selected "$cache_root" -xdev -maxdepth 1 -type f)"
resource_bytes="$(sum_selected "$cache_root" -xdev -mindepth 1 -maxdepth 1 -type d -name '?')"
nested_bytes="$(sum_selected "$cache_root" -xdev -mindepth 1 -maxdepth 1 -type d ! -name '?')"
flat_count="$(find "$cache_root" -xdev -maxdepth 1 -type f -printf '.' | wc -c)"
resource_root_count="$(find "$cache_root" -xdev -mindepth 1 -maxdepth 1 -type d -name '?' -printf '.' | wc -c)"
nested_root_count="$(find "$cache_root" -xdev -mindepth 1 -maxdepth 1 -type d ! -name '?' -printf '.' | wc -c)"

age_bucket_totals() {
  # The entries themselves can retain credentials in legacy URL-derived names.
  # Send only timestamps and byte counts to awk; it emits aggregate totals.
  find "$cache_root" -xdev -type f -printf '%T@ %s\0' | awk -v now="$(date +%s)" '
    BEGIN {
      RS = "\0"
      buckets["under_1_day"] = 0
      buckets["1_to_7_days"] = 0
      buckets["8_to_30_days"] = 0
      buckets["31_to_90_days"] = 0
      buckets["over_90_days"] = 0
    }
    {
      split($0, fields, " ")
      age = now - int(fields[1])
      bytes = fields[2]
      if (age < 86400) bucket = "under_1_day"
      else if (age < 604800) bucket = "1_to_7_days"
      else if (age < 2592000) bucket = "8_to_30_days"
      else if (age < 7776000) bucket = "31_to_90_days"
      else bucket = "over_90_days"
      buckets[bucket] += bytes
      counts[bucket]++
    }
    END {
      print "cache_age_buckets_bytes_file_counts"
      print "under_1_day " buckets["under_1_day"] " " counts["under_1_day"] + 0
      print "1_to_7_days " buckets["1_to_7_days"] " " counts["1_to_7_days"] + 0
      print "8_to_30_days " buckets["8_to_30_days"] " " counts["8_to_30_days"] + 0
      print "31_to_90_days " buckets["31_to_90_days"] " " counts["31_to_90_days"] + 0
      print "over_90_days " buckets["over_90_days"] " " counts["over_90_days"] + 0
    }
  '
}

printf 'cache_classes_bytes_root_entry_counts\n'
printf 'flat %s %s\nresource_shaped %s %s\nnested_or_stateful %s %s\n' \
  "$flat_bytes" "$flat_count" "$resource_bytes" "$resource_root_count" "$nested_bytes" "$nested_root_count"
age_bucket_totals
printf 'cache_root_owner_mode\n'
stat --format='%U:%G:%a' "$cache_root"
