package org.open4goods.datareference.model.registry;

/**
 * Lifecycle of a Git-authored O4G concept.
 */
public enum RegistryLifecycle {
    /** Available for new mappings and resolution. */
    ACTIVE,
    /** Kept for historical projections but not selected for new mappings. */
    DEPRECATED,
    /** Retained only so a historical registry version remains interpretable. */
    RETIRED
}
