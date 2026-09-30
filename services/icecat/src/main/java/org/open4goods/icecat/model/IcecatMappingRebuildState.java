package org.open4goods.icecat.model;

/** Lifecycle state of a requested read-only registry projection rebuild. */
public enum IcecatMappingRebuildState {
    QUEUED,
    RUNNING,
    SUCCEEDED,
    FAILED
}
