---
title: "Production log triage"
normative: false
audience: PROJECT_SCOPED
---

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
