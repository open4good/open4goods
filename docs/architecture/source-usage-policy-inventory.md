---
title: "Source usage policy inventory"
normative: false
audience: PROJECT_SCOPED
---

# Source usage policy inventory

The Git-authored [policy inventory](../../services/data-reference/src/main/resources/policy/source-usage-policies.json)
is the publication authority for source evidence, and the only place to read a
per-source fact: every record carries its content types and granted surfaces,
evidence links, effective period, retention, attribution, media-cache setting and
review state. [ADR-0010](../adr/0010-source-neutral-product-reference.md) states
how a policy is read; `SourceUsagePolicyRegistryTest` is its executable reading,
over both this deny inventory and a reviewed fixture.

Each policy grants surfaces per content type through `surfaceGrants`, a map from
content type to its allowed projection surfaces, rather than a flat content-type
set crossed with a flat surface set. A source can clear its identifiers (GTIN,
MPN) for the `ODBL_EXPORT` surface without that clearing its attributes, text or
media for the same surface - a distinction a cartesian product could not
express, and that a second policy record cannot supply either, because a source
record carries exactly one policy reference. A content type present as a key
with an empty surface set is still reviewed for `allowsUse` (e.g. a named use
such as AI training); it is simply published nowhere.

A policy also carries a deny-by-default `derivativeLicence` (e.g. the Icecat
share-alike obligation), a deny-by-default `prohibitedUses` set (e.g.
`AI_TRAINING`, `SYNTHETIC_CONTENT_GENERATION`), and an attribution
`asIsDisclaimerRequired` flag for the Fair Use Policy disclaimer. The mirrored
`allowsUse` predicate on the policy and the registry checks one named use, and
`DeterministicResolutionService` consults it to drop a prohibited-use source
from a derivation before any resolved value is produced.

## Ratified matrix (GOU-95, 2026-09-29)

The [GOU-95](/GOU/issues/GOU-95) `decisions` document is the clause-by-clause
authority; [GOU-105](/GOU/issues/GOU-105) implements its eleven rows, each
`reviewState: REVIEWED` (a total refusal is decided, not unreviewed).
`ODBL_EXPORT` stays closed on every source at this stage. The generic
`merchant-feed` row is replaced by six per-network rows, since one policy
record cannot carry six publisher agreements.

| Source / version | Content types granted | Surfaces | Redistribution | Retention |
|---|---|---|---|---|
| `eprel-public-api` / 2 | IDENTITY, CLASSIFICATION, ATTRIBUTE | NUDGER_WEB, B2B_API | VALUE_ADDED_ONLY | P35D |
| `icecat-open-content` / 2 | IDENTITY, CLASSIFICATION, ATTRIBUTE, TEXT, MEDIA, RELATION | NUDGER_WEB | ALLOWED + SHARE_ALIKE | P365D |
| `icecat-full-subscription` / 1 | - (total refusal) | - | PROHIBITED | PT0S |
| `merchant-feed.awin` / 1 | IDENTITY, ATTRIBUTE, OFFER, PRICE | NUDGER_WEB, B2B_API | VALUE_ADDED_ONLY | P1825D |
| `merchant-feed.effiliation` / 1 | IDENTITY, ATTRIBUTE, OFFER, PRICE | NUDGER_WEB, B2B_API | VALUE_ADDED_ONLY | P1825D |
| `merchant-feed.tradetracker` / `.kwanko` / `.webgains` / `.cj` - 1 | - (total refusal) | - | PROHIBITED | PT0S |
| `legacy-product-backup` / 3 | IDENTITY | NUDGER_WEB | VALUE_ADDED_ONLY | P1825D |
| `legacy-backup-import` / 1 | - (total refusal) | - | PROHIBITED | PT0S |
| `amazon-paapi-quarantine` / 2 | - (total refusal, `revokedAt` set) | - | PROHIBITED | PT0S |

EPREL's B2B grant is free-with-account (T&C 4§2(a)). Icecat's OPL forbids
charging for network access, so `B2B_API` stays refused despite
`redistribution: ALLOWED`; `prohibitedUses` excludes synthetic content
generation only, keeping AI-training embeddings allowed. Awin/Effiliation's
publisher agreements cover `OFFER`/`PRICE` only, so `TEXT`/`MEDIA` stay
refused. The other four networks have no publisher agreement. Amazon's PA-API
5 is deprecated and its 2026-04-14 Program Policies prohibit resale.

Icecat's two prior evidence URIs were unlicensed marketing pages, replaced by the verified
[Open Content License v1.4](https://iceclog.com/open-content-license/), [Fair Use Policy](https://iceclog.com/open-icecat-fair-use-policy/)
and [Disclaimer](https://icecat.biz/ssr/en/menu/disclaimer).

Lead Tech/Goulven resolved the `legacy-product-backup` coherence question
raised while authoring this inventory: `redistribution` moved from
`PROHIBITED` to `VALUE_ADDED_ONLY` (version 3) so the ratified
`IDENTITY → NUDGER_WEB` `surfaceGrants` entry is actually effective, instead
of being shadowed by `allows()`'s redistribution gate, mirroring how
`icecat-open-content` reasons through the same interaction.

GOU-165: `scripts/verify/check_usage_policy_registry.py` rejects a code-cited reference missing here (e.g. `legacy-backup-import` / 1) or a deleted `(policyId, version)` row; `SourceUsagePolicyRegistry` now `WARN`s on an unresolved one. `legacy-product-backup`'s `legalReviewDate`, `2026-10-01`, is the actual GOU-105 ratification date, not a new rights period.
