package org.open4goods.datareference.model.projection;

import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.open4goods.datareference.model.CanonicalAttributeId;
import org.open4goods.datareference.model.Gtin;
import org.open4goods.datareference.model.ProjectionSurface;
import org.open4goods.datareference.model.registry.RegistryVersion;
import org.open4goods.datareference.model.resolution.ResolvedValue;

/**
 * One product document, for one surface.
 *
 * <p>Built per surface rather than filtered on read: the same product legally
 * carries different content on the public site, the B2B API and the open-data
 * export, and a single document filtered at read time is one forgotten filter
 * away from publishing content that was never licensed for that surface.
 *
 * @param gtin product identity, the leaf identity of the model
 * @param surface surface this document may be served on
 * @param registryVersion registry version the values were resolved against
 * @param builtAt instant the document was assembled
 * @param resolvedValues reference values, ordered by canonical attribute
 * @param slices contributions from other domains, keyed by slice name
 */
public record ProductReferenceProjection(
        Gtin gtin,
        ProjectionSurface surface,
        RegistryVersion registryVersion,
        Instant builtAt,
        List<ResolvedValue> resolvedValues,
        Map<String, DomainSlice> slices) {

    /**
     * Validates the document and rejects a repeated canonical attribute.
     */
    public ProductReferenceProjection {
        Objects.requireNonNull(gtin, "gtin must not be null");
        Objects.requireNonNull(surface, "surface must not be null");
        Objects.requireNonNull(registryVersion, "registryVersion must not be null");
        Objects.requireNonNull(builtAt, "builtAt must not be null");
        resolvedValues = List.copyOf(Objects.requireNonNull(resolvedValues, "resolvedValues must not be null"));
        Set<CanonicalAttributeId> seen = new HashSet<>();
        for (ResolvedValue resolved : resolvedValues) {
            Objects.requireNonNull(resolved, "resolvedValues must not contain null");
            if (!seen.add(resolved.attribute())) {
                throw new IllegalArgumentException("duplicate resolved attribute: " + resolved.attribute());
            }
        }
        // LinkedHashMap, not Map.copyOf: an immutable map's iteration order is
        // unspecified and salted per JVM run, so a document with two slices would
        // serialize differently between runs and no frozen document could pin it.
        Map<String, DomainSlice> checked = new LinkedHashMap<>();
        for (Map.Entry<String, DomainSlice> entry : Objects
                .requireNonNull(slices, "slices must not be null").entrySet()) {
            DomainSlice slice = Objects.requireNonNull(entry.getValue(), "slices must not contain null");
            if (!slice.name().equals(entry.getKey())) {
                throw new IllegalArgumentException(
                        "slice key " + entry.getKey() + " does not match slice name " + slice.name());
            }
            checked.put(entry.getKey(), slice);
        }
        slices = Collections.unmodifiableMap(checked);
    }
}
