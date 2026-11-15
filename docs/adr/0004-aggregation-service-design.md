# ADR 0004 - Aggregation Service Design: Manual Instantiation vs. Spring Beans

**Status**: Accepted (updated 2026-11-15, GOU-2280: the standard chain is shared)  
**Date**: 2026-06-05  
**Context**: Aggregation pipeline architecture review

---

## Context

The `AggregationFacadeService` assembles two kinds of aggregator pipelines:

1. **StandardAggregator** - realtime + sanitisation (chains 7 services)
2. **ScoringBatchedAggregator** - batch scoring (chains 6 services)

Services are created with `new XxxAggregationService(...)` and then
Spring-autowired post-construction via `AutowireCapableBeanFactory.autowireBean()`.
This pattern was chosen because:

- Each invocation mode (realtime, sanitisation, classification, scoring) needs a
  **fresh, independent** service chain to avoid cross-run state leakage.
- Several scoring services hold mutable batch-scoped state (`batchDatas`,
  `absoluteCardinalities`, `valueFrequencies` in `AbstractScoreAggregationService`)
  that must be isolated between vertical scoring runs.

---

## Problem

The current approach has three consequences:

1. **Spring lifecycle is bypassed**: `@PostConstruct`, AOP proxies, and
   `@ConditionalOnProperty` beans do not work on manually-created instances.
2. **Stateful services are opaque**: The per-batch state fields feel like they belong
   to the aggregator run, not to the service class itself. This makes services hard
   to reason about and impossible to share as singletons.
3. **`AutowireCapableBeanFactory`** is a low-level escape hatch; idiomatic Spring
   favours constructor injection with registered beans.

---

## Decision

**Keep manual instantiation for now** (no breaking changes). Adopt a two-step
migration plan:

### Step 1 - Extract run-scoped state into a context object (next sprint)

Move `batchDatas`, `absoluteCardinalities`, and `valueFrequencies` out of
`AbstractScoreAggregationService` into a `BatchScoringContext` value object.
Pass the context through `init(datas, context)` / `onProduct(p, vConf, context)` /
`done(datas, vConf, context)`.

Result: service classes become **stateless** between runs. A single instance can
be safely reused across vertical scoring runs.

### Step 2 - Register services as `@Component` prototype beans (following sprint)

Once stateless, services can be registered with `@Scope("prototype")`. The factory
assembles pipelines by calling `applicationContext.getBean(XxxService.class)` instead
of `new`. Spring then manages injection, lifecycle, and conditionals normally.

`AutowireCapableBeanFactory.autowireBean()` calls can be dropped.

### Non-goal: Spring Batch

Spring Batch (jobs, steps, item processors) is **not** the right fit:
- The pipeline is not transactional in the XA sense.
- The existing queue/worker architecture already handles parallelism and retry.
- Introducing Spring Batch would add significant accidental complexity for no gain.

### Non-goal: Event-driven rewire

`ApplicationEventPublisher` would scatter the service chain across listeners,
making the execution order implicit and debugging harder.

---

## Consequences

- **Short-term**: No change. Manual instantiation stays; this ADR documents the intent.
- **Medium-term**: `BatchScoringContext` refactor. All scoring-service tests become
  easier to write (no state leaking between test invocations).
- **Long-term**: True singleton services, standard `@Bean` definitions, full Spring
  lifecycle support.

---

## Update 2026-11-15 - One shared standard chain (GOU-2280)

The standard chain did not need a fresh instance per invocation mode. Its 7 services
hold only `final` fields towards thread-safe singletons and immutable constants, and none
of them overrides `init`, `done` or `close`. The 4 completion chains were already called by
`api.completion-concurrency` (10) virtual threads each.

- `AggregationFacadeService` builds the standard chain **once**. `getStandardAggregator()`
  hands the same instance to the realtime ingestion, the completion services (Icecat,
  EPREL, Wikidata, Amazon), the batch aggregation and the on-demand sanitisation. Before,
  there were 6 copies or more: realtime, one per completion service, one per batch call.
  The classification-only chain (identity + taxonomy) is also built once.
- A service of the standard chain must therefore hold **no mutable state**. Run-scoped
  state belongs to the scoring chain.
- `AutowireCapableBeanFactory.autowireBean()` is dropped. On an aggregator it injected
  only the factory itself, which `AbstractAggregator.beforeStart()` then used to autowire
  the services. No service has an injection point (`@Autowired`, `@Value`, `@Inject`,
  `@Resource`), so nothing was ever injected into them. Also dropped: `beforeStart()`, which
  called `init` on services that do not override it; the lifecycle with no override
  (`close()` and `Closeable` on aggregators and services); and the facade's
  `@PreDestroy shutdown()`. Services receive their collaborators through their constructors.
- The scoring chain is still built for each run: Step 1 below (`BatchScoringContext`)
  has not been done, so the scoring services keep their run-scoped state.
- Consequence on logs: the standard chain writes to the `realtime` aggregation log
  whatever the caller. `sanitisation.log` and the per-completion `*-aggregation.log`
  files are no longer written.

Step 2 (prototype beans) is moot for the standard chain. It still applies to the
scoring chain once Step 1 is done.

---

## Related

- TODO in `AggregationFacadeService`: state machine to prevent concurrent vertical launches.
- `AbstractScoreAggregationService.init()` currently doubles as a reset guard; it will
  be simplified once context is extracted.
