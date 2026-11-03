package org.open4goods.api.services;

import java.time.Instant;

/**
 * Snapshot of the asynchronous datasource purge (GOU-1592).
 *
 * @param state          IDLE before any run since startup, RUNNING, COMPLETED (report present) or FAILED
 *                       (unexpected error, see {@code error})
 * @param datasourceName datasource of the current or last run, null when IDLE
 * @param dryRun         mode of the current or last run
 * @param startedAt      start of the current or last run
 * @param finishedAt     end of the last run, null while RUNNING
 * @param report         report of the last run, null while RUNNING or FAILED
 * @param error          cause of a FAILED run
 */
public record DatasourcePurgeStatus(
		State state,
		String datasourceName,
		boolean dryRun,
		Instant startedAt,
		Instant finishedAt,
		DatasourcePurgeReport report,
		String error) {

	public enum State { IDLE, RUNNING, COMPLETED, FAILED }

	public static DatasourcePurgeStatus idle() {
		return new DatasourcePurgeStatus(State.IDLE, null, true, null, null, null, null);
	}
}
