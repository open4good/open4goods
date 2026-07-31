package org.open4goods.api.services;

/**
 * Action applied to cached files identified as orphaned by
 * {@link BatchService#cleanOrphanResources(ResourceCleanupMode)}.
 */
public enum ResourceCleanupMode {

	/**
	 * Classify and report only. No file is touched. This is the default, and the only
	 * mode that should be used to validate a classification change against production data.
	 */
	DRY_RUN,

	/**
	 * Move orphans to the deletion folder for manual inspection. Note that this frees no
	 * disk space when both folders live on the same filesystem: space is only reclaimed
	 * once {@link BatchService#purgeDeletionFolder()} removes the parked files.
	 */
	MOVE,

	/**
	 * Delete orphans in place. Reclaims space immediately, with no way back.
	 */
	DELETE
}
