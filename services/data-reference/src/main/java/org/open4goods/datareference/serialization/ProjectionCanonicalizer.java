package org.open4goods.datareference.serialization;

import java.util.Objects;

import org.open4goods.datareference.model.ProjectionSurface;
import org.open4goods.datareference.model.projection.ProductReferenceProjectionEnvelope;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Produces the stable representation used to compare projection replays.
 *
 * <p>The representation includes resolved values, provenance and every
 * versioned replay input. It omits {@code builtAt}, which tells operators when
 * a worker delivered a result rather than what that result means. A new expiry
 * instant remains part of {@code replayInputs}; it is an intentional new input,
 * not an operational timestamp to erase.
 */
public final class ProjectionCanonicalizer {

    private ProjectionCanonicalizer() {
    }

    /**
     * Serializes a projection without operational delivery timestamps.
     *
     * @param projection complete three-surface projection
     * @return canonical contract bytes suitable for byte-for-byte comparison
     */
    public static byte[] canonicalBytes(ProductReferenceProjectionEnvelope projection) {
        Objects.requireNonNull(projection, "projection must not be null");
        JsonNode tree = DataReferenceJson.mapper().valueToTree(projection);
        if (!(tree instanceof ObjectNode root) || !(root.get("components") instanceof ObjectNode components)) {
            throw new IllegalStateException("projection serialization did not produce an envelope object");
        }
        for (ProjectionSurface surface : ProjectionSurface.values()) {
            JsonNode component = components.get(surface.name());
            if (!(component instanceof ObjectNode object)) {
                throw new IllegalStateException("projection serialization is missing " + surface + " component");
            }
            object.remove("builtAt");
        }
        try {
            return DataReferenceJson.mapper().writeValueAsBytes(root);
        } catch (RuntimeException exception) {
            throw new IllegalStateException("could not serialize canonical product-reference projection", exception);
        }
    }
}
