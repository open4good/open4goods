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

Today every record is `UNREVIEWED` and grants no surface, so nothing published
derives from one. An API, a legacy public page or a source's receipt of merchant
content is supporting evidence, not an O4G redistribution approval.

## Owner review packet

Each unreviewed record is a packet: source, version, content/surface matrix and
evidence links as recorded in the JSON, plus proposed effective dates, retention,
attribution wording and media-cache setting. The owner answers with an exact
reviewed policy version, a revocation timestamp, or an explicit denial -- tracked
on that source's Paperclip issue, which keeps a pending decision out of the corpus.
