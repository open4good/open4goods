package org.open4goods.api.services;

import java.time.Instant;

/**
 * Snapshot of the asynchronous orphan resource cleanup (GOU-1142).
 *
 * @param state      IDLE before any run since startup, RUNNING, COMPLETED (report present, possibly aborted) or
 *                   FAILED (unexpected error, see {@code error})
 * @param mode       mode of the current or last run, null when IDLE
 * @param startedAt  start of the current or last run
 * @param finishedAt end of the last run, null while RUNNING
 * @param report     report of the last run, null while RUNNING or FAILED
 * @param error      cause of a FAILED run
 */
public record ResourceCleanupStatus(
		State state,
		ResourceCleanupMode mode,
		Instant startedAt,
		Instant finishedAt,
		ResourceCleanupReport report,
		String error) {

	public enum State { IDLE, RUNNING, COMPLETED, FAILED }

	public static ResourceCleanupStatus idle() {
		return new ResourceCleanupStatus(State.IDLE, null, null, null, null, null);
	}
}
