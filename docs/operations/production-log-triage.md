# Production log triage

This runbook defines the production log signals that require action and the
safe response for each. It applies to `/opt/open4goods/logs` and the frontend
SSR container logs.

## Immediate signals

| Signal | Threshold | Owner | Response |
| --- | --- | --- | --- |
| B2B API 5xx | Any sustained occurrence | B2B API | Correlate Nginx access log with `nohup-b2b-api.log`; verify auth routes return 401, not 500, for unauthenticated requests. |
| Frontend 502/504 | Any deployment burst or more than 5 in 5 minutes | Frontend and operations | Keep the active color running, inspect container restart count and Nginx upstream errors, then roll back the handover if the new color is not stable. |
| Remote resource URL exposes query values | Any occurrence | Security and service owner | Treat credentials as disclosed, rotate them with approval, and redact the logging call before redeploying. |
| EPREL no-match volume | More than 10 per minute after refresh throttling | API data pipeline | Check datasource refresh timestamps and upstream coverage; do not repeatedly query unchanged negative results. |
| Image decode/hash failures | More than 10 per minute | API data pipeline | Check source format and ImageMagick fallback; preserve a terminal resource status instead of retrying unchanged files. |
| `Products index mapping: ... failed after 3 attempts` (ERROR, api) | Any occurrence | API | The api runs with the mapping already in the index. Read the `Caused by` of the three WARN lines before it, then follow [products index mapping](product-index-mapping.md#after-an-error). |

## Spring services: process, stdout and heap dumps

`publish-jars.sh` (open4goods-config, `bin/`) deploys and restarts the Spring
services; `jvm-launch.sh` builds the JVM command line and runs it in the
foreground (GOU-1912). The process manager is chosen per service:

- `systemctl is-enabled nudger-jvm@<service>` answers `enabled` (unit
  installed by open4good/fleet): systemd starts and stops the JVM and
  restarts it after a crash or an `OutOfMemoryError`, with the JAR in place.
  Read its state with `systemctl status nudger-jvm@<service>` and its
  restarts with `journalctl -u nudger-jvm@<service>`. The service is never
  launched with `nohup`.
- otherwise: `nohup jvm-launch.sh <env> <service>`, as before. Nothing restarts a JVM that
  dies until the next `publish-jars.sh`.

In both modes stdout goes to `logs/nohup-<service>.log` and the PID to
`run/pid-<service>.file`. The heap dump of an `OutOfMemoryError` (`api`,
`ui`, `front-api`) goes to `/opt/open4goods/heapdumps/<service>/` (0700).
At each start, an automatic restart included, `jvm-launch.sh` keeps the
first dump of a series and the most recent ones, at most 3, and deletes
those older than 7 days (`jvmdiag prune` when the host declares a `jvmdiag`
target for the service). With less than 1.5 × `-Xmx` free on that disk, the
JVM starts without a heap dump on OOM and its log carries a `WARNING`. A
dump holds the application's secrets: it never leaves the machine.

## Source and retention policy

- The canonical brand-company dataset is
  `open4good/brands-company-mapping`; per-company documents live under
  `brands/{id}.json`.
- Never log remote URL query values. They can contain signed URLs or access
  credentials.
- Nginx access logs are rotated daily or at 256 MB and reopened after rotation.
  Application, batch, crawler, and JVM logs are rotated daily or at 128 MB
  with `copytruncate` because their processes retain file handles.
- Retain 14 compressed rotations. Do not delete current production logs or
  rotate credentials without explicit production approval.

## Deployment verification

1. Run the frontend publish script in check-only mode before a handover.
2. Start only the inactive frontend color and require two successful local
   probes with an unchanged Docker restart count before switching Nginx.
3. Check Nginx errors and frontend 5xx responses after the switch, before
   stopping the old color.
4. Record the active color, deployment timestamp, and any rollback in the
   deployment log.
