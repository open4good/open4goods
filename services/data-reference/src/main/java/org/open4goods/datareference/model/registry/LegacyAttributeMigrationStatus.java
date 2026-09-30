package org.open4goods.datareference.model.registry;

/**
 * Reviewed disposition of one legacy vertical attribute configuration.
 *
 * <p>A migration is never implicit: an existing configuration must be carried
 * to an O4G attribute, deliberately merged into one, or retired with an
 * explicit replacement and rationale.
 */
public enum LegacyAttributeMigrationStatus {
    /** The legacy configuration has a one-to-one O4G attribute replacement. */
    MIGRATED,
    /** The legacy configuration is represented by an existing shared O4G attribute. */
    MERGED,
    /** The legacy configuration is no longer used and has an explicit successor. */
    RETIRED
}
