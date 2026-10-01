# Shared CSV ingestion fixtures (GOU-111)

These fixtures are the shared verification base for merchant feed adapters
(GOU-110, GOU-112, GOU-113) and for GOU-43. They are read by
`CsvIngestionFixturesTest` in
`org.open4goods.services.feedservice.definition`.

Each fixture is exercised today at the `FeedColumnResolver` / `FeedDefinition`
level only: column resolution (known/unknown headers, missing unit columns)
and raw CSV value fidelity (quoting, decimal literals, multi-valued cells kept
intact). No merchant-feed `SourceRecordHead` / `SourceAssertion` /
`IngestionCheckpoint` adapter exists yet (the reference/offer split issue that
was expected to deliver one hadn't landed when this fixture set was built),
so none of the fixtures below are wired into an actual ingestion run yet.

Fixtures whose documented behavior needs that adapter before it can be
asserted end-to-end, and what that future test must add:

- `duplicate-identical-rows.csv` - assert the adapter emits exactly one
  mutation for the two identical rows (deduplicated), not two.
- `conflicting-rows-same-key.csv` - assert the adapter quarantines both rows
  under the shared key instead of applying last-row-wins.
- `multi-valued-columns.csv` - assert the adapter splits the pipe-delimited
  `category` cell into ordinal-distinguished `SourceAssertion`s on the same
  canonical field, per `SourceAssertion.Coordinate`.
- `unchanged-offer-poll-1.csv` / `-poll-2.csv` - assert the adapter produces
  no new price-history point when the offer is unchanged between the two
  polls.
- `price-change-poll-1.csv` / `-poll-2.csv` - assert the adapter records a new
  price-history point for the changed offer.
- `explicit-deletion.csv` - assert the adapter removes exactly the named
  `SourceRecordHead`(s) and nothing else.
- `interrupted-full-feed-previous-complete.csv` /
  `-truncated.csv` - assert that, when `IngestionCheckpoint` shows the FULL
  read did not complete, the records present in the previous complete run but
  absent from the truncated one are NOT marked deleted.
- `usage-policy-change.csv` - assert that reclassifying the `warranty`
  column's `SourceContentType` changes only usage-policy filtering on the
  resulting `SourceAssertion` and never writes to `Product` directly.

The remaining fixtures (`quoting-edge-cases.csv`, `localized-decimals.csv`,
`missing-language.csv`, `missing-unit-column.csv`) are fully asserted today:
their documented behavior is resolved entirely by `FeedColumnResolver` and raw
CSV parsing, with no adapter dependency.
