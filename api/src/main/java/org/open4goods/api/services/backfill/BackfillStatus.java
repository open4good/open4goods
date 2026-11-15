package org.open4goods.api.services.backfill;

import java.time.Instant;

/**
 * Snapshot of the asynchronous backfill or restore (GOU-2266), also persisted as JSON after each batch.
 *
 * @param state          IDLE when no run is known, RUNNING, COMPLETED, STOPPED (stop requested or Elasticsearch 429,
 *                       see {@code report.stopReason}), FAILED (unexpected error, see {@code error}) or INTERRUPTED
 *                       (the api stopped while the run was RUNNING, read back at startup)
 * @param operation      operation of the current or last run, null when IDLE
 * @param dryRun         mode of the current or last run
 * @param restore        true when the current or last run is a restore from a backup file
 * @param startedAt      start of the current or last run
 * @param lastProgressAt last batch written to the status
 * @param finishedAt     end of the last run, null while RUNNING and when INTERRUPTED
 * @param report         counters, backup file and cursor: so far while RUNNING, at the last batch when STOPPED,
 *                       INTERRUPTED or FAILED, final when COMPLETED
 * @param error          cause of a FAILED run
 */
public record BackfillStatus(
		State state,
		String operation,
		boolean dryRun,
		boolean restore,
		Instant startedAt,
		Instant lastProgressAt,
		Instant finishedAt,
		BackfillReport report,
		String error) {

	public enum State { IDLE, RUNNING, COMPLETED, STOPPED, FAILED, INTERRUPTED }

	public static BackfillStatus idle() {
		return new BackfillStatus(State.IDLE, null, true, false, null, null, null, null, null);
	}

	static BackfillStatus running(String operation, boolean dryRun, boolean restore, Instant at,
			BackfillReport report) {
		return new BackfillStatus(State.RUNNING, operation, dryRun, restore, at, at, null, report, null);
	}

	/** @return this RUNNING status with the counters of a new batch */
	BackfillStatus withProgress(BackfillReport progress, Instant at) {
		return new BackfillStatus(state, operation, dryRun, restore, startedAt, at, finishedAt, progress, error);
	}

	/** @return the terminal status of this run */
	BackfillStatus finish(State finalState, BackfillReport finalReport, Instant at, String cause) {
		return new BackfillStatus(finalState, operation, dryRun, restore, startedAt, at, at, finalReport, cause);
	}

	/** @return this status, read back at startup, marked INTERRUPTED with its last counters and cursor */
	BackfillStatus interrupted() {
		return new BackfillStatus(State.INTERRUPTED, operation, dryRun, restore, startedAt, lastProgressAt, null,
				report, error);
	}
}
