package org.open4goods.api.services;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAccumulator;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;

/**
 * Per completion service counters of one batch pass over a vertical (GOU-2099): products processed, skipped and
 * failed, total and maximum processing time. Thread-safe, the completions of a vertical run concurrently.
 */
public class CompletionTimings {

	/** Counters of one completion service, in the order the services ran first. */
	private final Map<String, ServiceTimings> services = new ConcurrentHashMap<>();

	/** Order of first appearance of the services, kept for a stable summary. */
	private final Map<String, Integer> order = new ConcurrentHashMap<>();

	private final AtomicLong products = new AtomicLong();

	/**
	 * Records one call of a completion service.
	 *
	 * @param service   the completion service name
	 * @param processed {@code true} when the product was processed, {@code false} when its refresh rule skipped it
	 * @param nanos     the duration of the call, skip decision included
	 */
	public void record(String service, boolean processed, long nanos) {
		ServiceTimings timings = timings(service);
		if (processed) {
			timings.processed.increment();
			timings.processedNanos.add(nanos);
			timings.maxNanos.accumulate(nanos);
		} else {
			timings.skipped.increment();
			timings.skippedNanos.add(nanos);
		}
	}

	/**
	 * Records a completion service call that threw: the following services of the product do not run.
	 *
	 * @param service the completion service name
	 * @param nanos   the duration of the call until the failure
	 */
	public void recordFailure(String service, long nanos) {
		ServiceTimings timings = timings(service);
		timings.failed.increment();
		timings.processedNanos.add(nanos);
		timings.maxNanos.accumulate(nanos);
	}

	/**
	 * Counts one product whose completion is over.
	 *
	 * @return the number of products completed so far
	 */
	public long productDone() {
		return products.incrementAndGet();
	}

	/**
	 * @return the number of products completed so far
	 */
	public long products() {
		return products.get();
	}

	/**
	 * @param service the completion service name
	 * @return the number of products the service processed (failures included)
	 */
	public long processed(String service) {
		ServiceTimings timings = services.get(service);
		return timings == null ? 0 : timings.processed.sum() + timings.failed.sum();
	}

	/**
	 * @param service the completion service name
	 * @return the number of products the service skipped
	 */
	public long skipped(String service) {
		ServiceTimings timings = services.get(service);
		return timings == null ? 0 : timings.skipped.sum();
	}

	/**
	 * @param service the completion service name
	 * @return the total time spent by the service, in milliseconds
	 */
	public long totalMillis(String service) {
		ServiceTimings timings = services.get(service);
		return timings == null ? 0 : (timings.processedNanos.sum() + timings.skippedNanos.sum()) / 1_000_000;
	}

	/**
	 * One line per completion service, e.g.
	 * {@code EprelCompletionService: processed 812 (failed 0, avg 1240 ms, max 9811 ms), skipped 8208, total 1007 s}.
	 *
	 * @return the summary, services in the order they ran
	 */
	public String summary() {
		return services.keySet().stream()
				.sorted((a, b) -> Integer.compare(order.get(a), order.get(b)))
				.map(this::summary)
				.collect(Collectors.joining(" ; "));
	}

	private String summary(String service) {
		ServiceTimings timings = services.get(service);
		long processed = timings.processed.sum() + timings.failed.sum();
		long avgMillis = processed == 0 ? 0 : timings.processedNanos.sum() / processed / 1_000_000;
		long maxMillis = processed == 0 ? 0 : timings.maxNanos.get() / 1_000_000;
		return "%s: processed %d (failed %d, avg %d ms, max %d ms), skipped %d, total %d s".formatted(service,
				processed, timings.failed.sum(), avgMillis, maxMillis, timings.skipped.sum(),
				totalMillis(service) / 1000);
	}

	private ServiceTimings timings(String service) {
		return services.computeIfAbsent(service, name -> {
			order.putIfAbsent(name, order.size());
			return new ServiceTimings();
		});
	}

	private static final class ServiceTimings {
		private final LongAdder processed = new LongAdder();
		private final LongAdder skipped = new LongAdder();
		private final LongAdder failed = new LongAdder();
		private final LongAdder processedNanos = new LongAdder();
		private final LongAdder skippedNanos = new LongAdder();
		private final LongAccumulator maxNanos = new LongAccumulator(Math::max, 0);
	}
}
