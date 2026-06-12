package org.open4goods.services.reviewgeneration.dto;

/**
 * Local status for a DataForSEO source discovery job.
 */
public enum SourceDiscoveryJobStatus {
    SUBMITTED,
    POLLING,
    COMPLETED,
    FAILED
}
