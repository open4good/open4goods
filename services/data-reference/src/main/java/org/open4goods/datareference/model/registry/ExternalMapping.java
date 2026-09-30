package org.open4goods.datareference.model.registry;

import java.time.LocalDate;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * A reviewed, time-bounded mapping from a provider coordinate to an O4G concept.
 *
 * @param system provider-neutral system name, such as {@code icecat} or {@code eprel}
 * @param externalId opaque coordinate in that system
 * @param status review state; only {@link ExternalMappingStatus#REVIEWED} may resolve
 * @param effectiveFrom inclusive date on which the mapping became valid
 * @param effectiveTo exclusive end date, or {@code null} while current
 */
public record ExternalMapping(
        String system,
        String externalId,
        ExternalMappingStatus status,
        LocalDate effectiveFrom,
        LocalDate effectiveTo) {

    private static final Pattern SYSTEM = Pattern.compile("[a-z][a-z0-9-]{1,63}");

    /**
     * Validates the stable provider coordinate and effective interval.
     */
    public ExternalMapping {
        Objects.requireNonNull(system, "mapping system must not be null");
        system = system.trim();
        if (!SYSTEM.matcher(system).matches()) {
            throw new IllegalArgumentException("mapping system must be lower-case kebab-case: " + system);
        }
        Objects.requireNonNull(externalId, "mapping externalId must not be null");
        externalId = externalId.trim();
        if (externalId.isEmpty() || externalId.length() > 256) {
            throw new IllegalArgumentException("mapping externalId must contain 1 to 256 characters");
        }
        Objects.requireNonNull(status, "mapping status must not be null");
        Objects.requireNonNull(effectiveFrom, "mapping effectiveFrom must not be null");
        if (effectiveTo != null && !effectiveTo.isAfter(effectiveFrom)) {
            throw new IllegalArgumentException("mapping effectiveTo must be after effectiveFrom");
        }
    }

    /**
     * Returns whether this mapping has the same external coordinate as another.
     *
     * @param other mapping to compare
     * @return whether both mappings address the same provider coordinate
     */
    public boolean hasCoordinate(ExternalMapping other) {
        return system.equals(other.system) && externalId.equals(other.externalId);
    }

    /**
     * Returns whether the two effective intervals overlap.
     *
     * @param other mapping to compare
     * @return whether the intervals overlap
     */
    public boolean overlaps(ExternalMapping other) {
        boolean thisStartsBeforeOtherEnds = other.effectiveTo == null || effectiveFrom.isBefore(other.effectiveTo);
        boolean otherStartsBeforeThisEnds = effectiveTo == null || other.effectiveFrom.isBefore(effectiveTo);
        return thisStartsBeforeOtherEnds && otherStartsBeforeThisEnds;
    }

    /**
     * Returns whether this mapping is effective on the supplied date.
     *
     * @param date date to test
     * @return whether the inclusive/exclusive interval contains the date
     */
    public boolean isEffectiveOn(LocalDate date) {
        Objects.requireNonNull(date, "date must not be null");
        return !date.isBefore(effectiveFrom) && (effectiveTo == null || date.isBefore(effectiveTo));
    }
}
