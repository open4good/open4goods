package org.open4goods.datareference.model.registry;

/**
 * Review state of an external-provider mapping.
 */
public enum ExternalMappingStatus {
    /** A reviewer has approved the mapping for the stated interval. */
    REVIEWED,
    /** The mapping is recorded for investigation and cannot drive resolution. */
    PROPOSED,
    /** The provider coordinate is known not to mean this O4G concept. */
    REJECTED
}
