package org.open4goods.datareference.model.projection;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

import org.open4goods.datareference.model.Gtin;
import org.open4goods.datareference.model.ProjectionSurface;

/**
 * The one stored read-model document for a normalized GTIN.
 *
 * <p>Each surface is composed and stored independently, but keeping the three
 * components in this envelope prevents an index from growing one document per
 * GTIN and surface. A reader selects its component before exposing fields; it
 * never filters a more permissive component at read time.
 *
 * @param gtin normalized product identity and document key
 * @param components exactly one independently resolved component per surface
 */
public record ProductReferenceProjectionEnvelope(
        Gtin gtin, Map<ProjectionSurface, ProductReferenceProjection> components) {

    /** Copies the three components and validates their identity and surface. */
    public ProductReferenceProjectionEnvelope {
        Objects.requireNonNull(gtin, "gtin must not be null");
        Map<ProjectionSurface, ProductReferenceProjection> checked = new EnumMap<>(ProjectionSurface.class);
        for (ProjectionSurface surface : ProjectionSurface.values()) {
            ProductReferenceProjection component = Objects.requireNonNull(
                    Objects.requireNonNull(components, "components must not be null").get(surface),
                    "missing projection component for " + surface);
            if (!gtin.equals(component.gtin()) || surface != component.surface()) {
                throw new IllegalArgumentException("projection component does not match its envelope coordinate");
            }
            checked.put(surface, component);
        }
        if (components.size() != checked.size()) {
            throw new IllegalArgumentException("projection envelope must contain only known surfaces");
        }
        components = Collections.unmodifiableMap(checked);
    }
}
