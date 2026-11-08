package org.open4goods.api.services;

import java.time.Instant;

/**
 * Snapshot of the asynchronous datasource purge (GOU-1592), also persisted as JSON at each progress step
 * (GOU-1815).
 *
 * @param state          IDLE when no run is known, RUNNING, COMPLETED, FAILED (unexpected error, see {@code error})
 *                       or INTERRUPTED (the api stopped while the run was RUNNING, read back at startup)
 * @param datasourceName datasource of the current or last run, null when IDLE
 * @param dryRun         mode of the current or last run
 * @param startedAt      start of the current or last run
 * @param lastProgressAt last progress step written (start of the run, then every progress interval, then the end)
 * @param finishedAt     end of the last run, null while RUNNING and when INTERRUPTED
 * @param report         counters of the run: so far while RUNNING, at the last progress step when INTERRUPTED or
 *                       FAILED, final when COMPLETED
 * @param error          cause of a FAILED run
 */
public record DatasourcePurgeStatus(
		State state,
		String datasourceName,
		boolean dryRun,
		Instant startedAt,
		Instant lastProgressAt,
		Instant finishedAt,
		DatasourcePurgeReport report,
		String error) {

	public enum State { IDLE, RUNNING, COMPLETED, FAILED, INTERRUPTED }

	public static DatasourcePurgeStatus idle() {
		return new DatasourcePurgeStatus(State.IDLE, null, true, null, null, null, null, null);
	}

	/** @return this RUNNING status with the counters of a new progress step */
	DatasourcePurgeStatus withProgress(DatasourcePurgeReport progress, Instant at) {
		return new DatasourcePurgeStatus(state, datasourceName, dryRun, startedAt, at, finishedAt, progress, error);
	}

	/** @return the terminal status of this run */
	DatasourcePurgeStatus finish(State finalState, DatasourcePurgeReport finalReport, Instant at, String cause) {
		return new DatasourcePurgeStatus(finalState, datasourceName, dryRun, startedAt, at, at, finalReport, cause);
	}

	/** @return this status, read back at startup, marked INTERRUPTED with its last counters and progress date */
	DatasourcePurgeStatus interrupted() {
		return new DatasourcePurgeStatus(State.INTERRUPTED, datasourceName, dryRun, startedAt, lastProgressAt, null,
				report, error);
	}
}
