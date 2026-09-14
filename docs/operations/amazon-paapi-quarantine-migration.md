---
title: "Amazon PA-API quarantine migration"
normative: false
audience: PROJECT_SCOPED
---

# Amazon PA-API quarantine migration

`scripts/migration/quarantine_amazon_paapi.py` is an operator-triggered migration, separate from application startup.

Run `INVENTORY` first against an explicit legacy index or alias. Its private report has aggregate provenance counts only.
Exact `amazon-paapi` labels are proven PA-API; the shared `amazon.fr` label is ambiguous and excluded. URLs and broad Amazon
text matches are not provenance evidence.

`APPLY` uses separately provisioned explicit target and ASIN identity indexes. It copies independently identified merchant
records with PIT/search-after batches and optimistic retry; ambiguous records contribute only syntax-valid ASIN identities.
It leaves the source, alias, and cache unchanged. Checkpoint and cancellation files permit bounded resume or cancellation.

Before a production APPLY, record the inventory report, backup and rollback reference, target mappings, and post-run
reconciliation. Cache deletion is separate and uses an owner-reviewed exact-object manifest plus recovery record rather than a
shared-cache or substring purge.
