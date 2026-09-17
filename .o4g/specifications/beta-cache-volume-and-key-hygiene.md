# Beta cache volume cutover and cache-key containment

## Objective and boundaries

The beta root filesystem is below the configured disk-health margin while DiskB has materially more capacity. DiskB also
hosts Elasticsearch data, so this is a bounded cache recovery, not a wholesale copy. The target is a stable canonical
cache path at `/opt/open4goods/.cached` backed by DiskB, with at least 20 percent free on root and at least 30 percent
free on DiskB after the rehearsal. The latter leaves Elasticsearch room for ingestion, segment merges and recovery.

Only beta is in scope. Production deployment, production cache changes and production credential rotation are explicitly
out of scope.

## Data classification and safety rules

The cache root is shared. Treat these classes differently:

| Class | Examples | Cutover rule |
| --- | --- | --- |
| Product-resource shard | the exact three-level resource-key hierarchy | Run the guarded product-index dry run first; reclaim only confirmed orphans. |
| Reconstructible remote file | root-level remote and Icecat downloads | Do not copy legacy URL-derived names. Rebuild under SHA-256 names after the code release. |
| Stateful/recovery data | batch state, recovery material, uploads or any unknown named directory | Preserve until its owning service and recovery procedure explicitly validate it. |

The cache inventory reports only aggregate byte totals, counts, owner/mode summaries and coarse age buckets. It must never
print a filename, URL, query parameter, request header, cache-key input or secret. Treat existing legacy filenames as
sensitive metadata. Do not archive them in shell history, service logs, tickets, command output or test fixtures.

## Cache-key contract

Remote cache keys are lowercase hexadecimal SHA-256 digests of the complete request URL. Temporary files use only the
`tmp-` prefix followed by the same digest. This keeps names deterministic for cache hits without leaking any portion of
the URL. All diagnostic URL output uses the path-only redaction helper; error messages must identify a failure category
rather than concatenate an upstream URL.

There is deliberately no fallback lookup of legacy URL-derived files: checking or moving such a path perpetuates the
metadata disclosure. A missing old remote entry is re-fetched through its configured source. Tests cover equal inputs,
different query values, opaque output and redacted logs.

## Capacity gate

Before any move or deletion, record `df` capacity and calculate the maximum cache allocation:

```
diskb-cache-budget = diskb-size * 0.70 - diskb-current-used
```

The current beta baseline allows no more than approximately 450 GB of additional DiskB content. Cap the first cutover at
400 GB so normal churn retains a buffer. The plan must stop if the post-step forecast would put DiskB above 70 percent
used or root below 20 percent free. A successful product-resource orphan dry run is evidence, not authority to delete;
record the aggregate result and apply only the reviewed batch.

## Rehearsal procedure

1. Stop cache writers through their named beta systemd units; do not use process-name scripts.
2. Create the DiskB cache root with `open4goods` ownership and restrictive permissions. Stage only the reviewed safe
   class, preserving metadata, and compare aggregate byte and file counts. Do not emit path lists.
3. Install a systemd mount or bind unit whose canonical source is the DiskB cache root and whose destination is
   `/opt/open4goods/.cached`. Units that write this path require the mount and must not start without it.
4. Start the named units, prove `findmnt` resolves the canonical path to DiskB, check the scoped health endpoints and
   confirm newly generated remote cache names match only the SHA-256 form.
5. Keep the old root cache intact for the agreed observation window. Roll back by stopping writers, unmounting the bind,
   restoring the old path and restarting the prior release. Do not delete the old cache as part of a failed rehearsal.
6. After observation, reclaim only the reviewed root-volume classes, re-check both capacity thresholds, and attach a
   sanitized aggregate evidence line to the WorkOrder.

## Rollback and stop conditions

Immediately roll back and retain all source data if mount provenance is not DiskB, ownership differs from the service
account, any service writes to root instead, a health check regresses, DiskB approaches 70 percent used, or root remains
below 20 percent free. A credential-bearing legacy filename is a containment concern: do not copy it; replace the beta
credential through its existing owner procedure if it may still be valid.
