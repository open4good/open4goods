# ADR 0001: Enhanced EPREL Matching Logic with Score Resolution

## Status

Accepted

## Context

The EPREL completion service (`EprelCompletionService`) matches products with
their corresponding entries in the EPREL (European Product Registry for Energy
Labelling) database.

Previously, if multiple potential EPREL candidates were found after the initial
brand/category filter, the service applied `hasModelEvidence` to each candidate
and accepted the result only when exactly one candidate passed. When more than
one candidate passed, the service aborted and returned empty to prevent false
positives. This caused many valid matches to be missed when EPREL listed
multiple model variants that differed by a suffix, or when one candidate matched
the model name far more precisely than the others.

## Decision

Introduce a scoring function `getModelEvidenceScore` that replaces the binary
`hasModelEvidence` predicate and ranks candidates by match quality:

| Score | Condition |
|-------|-----------|
| 3 | Normalized model identifiers are equal, OR compact representations are equal |
| 2 | Product model contains EPREL model as a whole phrase, OR compact product model contains compact EPREL model (with min-length guard) |
| 1 | EPREL model contains product model as a whole phrase, OR compact EPREL model contains compact product model (with min-length guard) |
| 0 | No match |

`hasModelEvidence` delegates to `getModelEvidenceScore > 0` (no behaviour
change for existing callers).

When `selectUniqueResult` encounters several model-matching candidates, it:

1. Computes the highest score across those candidates.
2. Keeps only candidates that achieved that maximum score.
3. Accepts the result only when exactly one best candidate remains.

If ambiguity persists after scoring, the method still returns empty, preserving
the existing false-positive protection.

## Consequences

- **Higher completion rate**: variant matching and partially-overlapping model
  names are now resolved correctly.
- **Precision preserved**: a candidate is only accepted when it strictly
  outscores all other shortlisted candidates.
- **Test coverage**: the unit test suite (`EprelCompletionServiceTest`) was
  expanded to cover score-based resolution; all 6 tests pass.
- **Lint**: the stale en-dash character in a JSON test mock was normalized with
  `scripts/python/text_replacements.py --fix`.
