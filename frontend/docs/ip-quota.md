# IP quota composable (useIpQuota)

This document explains how the frontend caches IP-based quota information and how
to use the generic quota composable.

## Storage details

The composable persists the last known quota status in **localStorage** only.
No cookies are written. Cached entries are considered fresh for 1 hour.

- **Key**: `nudger-ip-quota-v1`
- **Value**: JSON object keyed by category
- **Entry shape**:
  ```json
  {
    "FEEDBACK_VOTE": {
      "used": 2,
      "remaining": 3,
      "limit": 5,
      "windowSeconds": 86400,
      "lastSync": 1717072000000
    }
  }
  ```

## Categories

The API currently exposes:

- `FEEDBACK_VOTE`
- `REVIEW_GENERATION`
- `CONTACT_MESSAGE` (reserved for future use)

## Usage

```ts
const { invalidateQuota, refreshQuota, getRemaining, getUsed } = useIpQuota()

await refreshQuota(IpQuotaStatusDtoCategoryEnum.FeedbackVote)
const remaining = getRemaining(IpQuotaStatusDtoCategoryEnum.FeedbackVote)
const used = getUsed(IpQuotaStatusDtoCategoryEnum.FeedbackVote)
```

The composable intentionally keeps read APIs (`getUsed`, `getRemaining`,
`getLimit`) separate from the fetch logic (`refreshQuota`) so components can
control when to sync with the backend.

Use `invalidateQuota` after successful actions (feedback votes, AI review
generation) to force a refresh and keep the cached count aligned with the
backend.
