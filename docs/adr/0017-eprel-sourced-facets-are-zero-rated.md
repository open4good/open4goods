---
title: "ADR 0017: EPREL-sourced facets are zero-rated"
status: accepted
normative: true
audience: PROJECT_SCOPED
decisions: []
---

# ADR 0017: EPREL-sourced facets are zero-rated

## Context

The B2B API is metered in credits, and the earlier facet catalogue priced
`product.energy` at 10 credits like any other exclusive facet. Its content comes
from the EPREL public API, whose Terms and Conditions permit redistribution that
adds value in one's own services (4§1(i)) but forbid selling the data as it is,
"even when complementary parameters are associated to each record" (4§2(a)). A
per-call price on an EPREL-sourced facet is that forbidden resale, whatever the
surrounding product does. Pricing therefore is not a commercial choice here, and
leaving it as one means the compliance boundary lives in a spreadsheet nobody
validates at startup.

## Decision

A facet whose content originates from EPREL is free of charge and never billable.
In `b2b-catalog.yml` such an entry declares `source: eprel`, `credits: 0` and
`billable-when: never`. `BillingCatalogProperties.Facet.isEprelFacetStructurallyFree()`
is an `@AssertTrue` guard, so an `eprel`-sourced facet carrying a non-zero price or
any other `billable-when` fails Bean Validation and the application does not start.
The serving path for such a facet does not call credit reservation or settlement at
all, so it cannot fail on an organization's balance.

Free is not anonymous. The endpoint still authenticates a `PDAPI_KEY` -- any active
API key on an account, with no paid plan required -- so use stays attributable, and
every response carries the EPREL attribution the API Terms require (4§3).

This supersedes any earlier pricing figure for an EPREL-sourced facet, including the
10 credits once listed for `product.energy`. `product.energy`
(`B2bProductService.getProductEnergy`, `ProductEprelMappingService`) is the reference
implementation.

## Consequences

The compliance boundary is enforced by the type system and startup validation rather
than by review discipline: a future EPREL-sourced facet cannot be priced by accident,
and a reviewer checking compliance reads one declared `source` per catalogue entry.
The cost is that provenance has to be declared honestly in the catalogue -- a facet
that mixes EPREL content under a different `source` defeats the guard, which is why
provenance belongs with the source usage policy of
[ADR-0010](0010-source-neutral-product-reference.md) and not with pricing.
