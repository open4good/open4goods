---
title: "Amazon PA-API completion quarantine"
normative: false
audience: PROJECT_SCOPED
---

# Amazon PA-API completion quarantine

Historical Amazon Product Advertising API v5 (PA-API) content is quarantined.
`AmazonCompletionService` remains present solely to make the legacy boundary
explicit: it does not construct a PA-API client, schedule a request, or process
a product, even if legacy configuration contains credentials.

The checked-in `amazon-paapi-quarantine` source policy denies every content type
on every public projection surface. The only future Amazon treatment is an ASIN
identity independently evidenced by a merchant source; a PA-API record itself is
not eligible for a projection.

## Legacy data boundary

The legacy `amazon.fr` datasource label is shared by PA-API and independent
merchant material. It therefore proves neither origin nor redistribution rights.
The controlled rebuilt-dataset import excludes the entire ambiguous legacy
`amazon.fr` slice rather than inferring a selective PA-API purge. Independent
Amazon merchant feeds remain separate source records and are not disabled by
this quarantine.

## Operations

The remaining migration delivery is designed around separate `INVENTORY` and
guarded beta `APPLY` stages. Its acceptance evidence records classified counts,
sanitized coordinates, checkpointed bounded PIT/search-after work, and the
absence of application-startup execution. Production APPLY remains a separate
owner operation under ADR-0013.

The historical PA-API API documentation is retained only as provenance:

- <https://webservices.amazon.com/paapi5/documentation/get-items.html>
- <https://webservices.amazon.co.uk/paapi5/documentation/best-programming-practices.html>
