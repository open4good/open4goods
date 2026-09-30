package org.open4goods.datareference.port;

/**
 * What a batch port does when one element cannot be processed.
 */
public enum ScanFailurePolicy {
    /** Abort the scan at the first failure and surface it to the caller. */
    FAIL_FAST,
    /** Record the failure, skip the element and continue. */
    SKIP_AND_REPORT
}
