# Verticals Agents Guide

> **Parent Guide**: [Root AGENTS.md](../AGENTS.md)  
> This guide **extends** the root conventions with Verticals-specific rules.

This module is part of the open4goods multi-module Maven project.

## Technology

- Java 21
- Spring Boot 4
- YAML configuration resources

## Directory structure

- `src/main/resources/verticals` - vertical definitions
- `src/main/resources/eprel-model-aliases/{vertical-id}/*.yml` - brand tables mapping commercial names and part
  numbers to the model registered in EPREL (for example Apple iPhone numbers), loaded into
  `VerticalConfig.eprelModelAliases`
- `src/test/java` - unit tests

## Purpose

The `verticals` module stores YAML configuration describing product verticals.

## Numeric criteria of subsets and sub-categories

`subsets`, `nudgeToolConfig.subsets` and the `activatedFilters` of
`src/main/resources/categories/{vertical-id}/*.yml` share the same operators, applied as written
by the listing (frontend `_subset-to-filters.ts`, front-api `SearchService`, GOU-2586):

| Operator | Elasticsearch | Bound |
|---|---|---|
| `LOWER_THAN` | `lt` | exclusive |
| `LOWER_THAN_OR_EQUAL` | `lte` | inclusive |
| `GREATER_THAN` | `gt` | exclusive |
| `GREATER_THAN_OR_EQUAL` | `gte` | inclusive |

- Adjacent buckets are half-open: `[a, b)` is `GREATER_THAN_OR_EQUAL a` + `LOWER_THAN b`, and the
  next bucket starts with `GREATER_THAN_OR_EQUAL b`. `LOWER_THAN b` followed by `GREATER_THAN b`
  leaves the products at exactly `b` out of both buckets.
- Reference count for a sub-category (the "Feasibility" comment of its YAML, the "fiche
  pratique" routine): `products-moustik`, `vertical = {vertical-id}`, `offersCount >= 1`,
  `excluded = false`, plus the `activatedFilters` with the operators above. Without
  `excluded = false` the count overstates the page total.

## Units and plausible ranges of numeric attributes

- An indexed numeric value is stored in the unit shown on the product page, named by the `unit` (or
  `suffix`) of `attributes/{KEY}.yml`. The frontend appends it after a no-break space and formats the
  number with the locale ("59,5 cm"). Lengths (WIDTH, HEIGHT, DEPTH) are in cm, WEIGHT in kg,
  WARRANTY in years, COOLING_CAPACITY in kW (GOU-2707).
- `UnitAwareNumericParser` converts to `parser.targetUnit`. A value sent without unit is read with
  the first of `parser.unitlessHints` that falls inside the vertical bounds, else with
  `defaultUnitHint`: Icecat sends a width as "600" (mm), EPREL as "60" (cm).
- `parsers.bounds.{KEY}` of a vertical (`min`, `max`, in the stored unit) is the plausible range of
  the attribute. A source outside it does not vote; an elected value outside it is not indexed, so
  neither the product page nor the ImpactScore uses it, and the aggregation log records it. Without a
  vertical bound, `parser.minValue` / `parser.maxValue` of the attribute apply.
- A category or subset threshold on such an attribute is written in the stored unit
  (`dishwasher_45.yml`: WIDTH from 43 to 47, in cm).

## Build and test this module only

From this directory:

```bash
mvn --offline clean install
```

Run only the tests with:

```bash
mvn --offline test
```

From the repository root you can also execute:

```bash
mvn --offline -pl verticals -am clean install
```
