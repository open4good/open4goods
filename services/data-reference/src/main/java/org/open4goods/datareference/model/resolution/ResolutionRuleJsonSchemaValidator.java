package org.open4goods.datareference.model.resolution;

import java.util.Set;
import java.util.stream.Collectors;

import org.open4goods.datareference.model.ProjectionSurface;

import tools.jackson.databind.JsonNode;

/**
 * Structural validator for the checked-in resolution-rule JSON Schema.
 *
 * <p>The schema, and this validator, allow only a per-(attribute, surface) rule
 * list: there is no document-level field a rule could use as a global or
 * cross-attribute default source order.
 */
final class ResolutionRuleJsonSchemaValidator {

    private static final Set<String> KNOWN_SURFACES = java.util.Arrays.stream(ProjectionSurface.values())
            .map(Enum::name).collect(Collectors.toUnmodifiableSet());

    private ResolutionRuleJsonSchemaValidator() {
    }

    static void validate(JsonNode root) {
        object(root, "$", Set.of("schemaVersion", "rules"), Set.of("schemaVersion", "rules"));
        string(root.path("schemaVersion"), "$.schemaVersion");
        array(root.path("rules"), "$.rules");
        for (int index = 0; index < root.path("rules").size(); index++) {
            validateRule(root.path("rules").path(index), "$.rules[" + index + "]");
        }
    }

    private static void validateRule(JsonNode node, String path) {
        object(node, path, Set.of("attribute", "surface", "version", "rankedSources", "regulatoryAuthority"),
                Set.of("attribute", "surface", "version", "rankedSources"));
        string(node.path("attribute"), path + ".attribute");
        string(node.path("surface"), path + ".surface");
        if (!KNOWN_SURFACES.contains(node.path("surface").asString())) {
            fail(path + ".surface names an unknown projection surface: " + node.path("surface").asString());
        }
        string(node.path("version"), path + ".version");
        stringArray(node.path("rankedSources"), path + ".rankedSources");
        if (node.path("rankedSources").isEmpty()) {
            fail(path + ".rankedSources must not be empty");
        }
        nullableString(node.path("regulatoryAuthority"), path + ".regulatoryAuthority");
    }

    private static void stringArray(JsonNode node, String path) {
        array(node, path);
        for (int index = 0; index < node.size(); index++) {
            string(node.path(index), path + "[" + index + "]");
        }
    }

    private static void object(JsonNode node, String path, Set<String> known, Set<String> required) {
        if (!node.isObject()) {
            fail(path + " must be an object");
        }
        node.propertyStream().forEach(entry -> {
            if (!known.contains(entry.getKey())) {
                fail(path + " has an unknown property: " + entry.getKey());
            }
        });
        required.forEach(field -> {
            if (!node.hasNonNull(field)) {
                fail(path + " is missing required property: " + field);
            }
        });
    }

    private static void array(JsonNode node, String path) {
        if (!node.isArray()) {
            fail(path + " must be an array");
        }
    }

    private static void string(JsonNode node, String path) {
        if (!node.isString() || node.asString().isBlank()) {
            fail(path + " must be a non-blank string");
        }
    }

    private static void nullableString(JsonNode node, String path) {
        if (!node.isMissingNode() && !node.isNull()) {
            string(node, path);
        }
    }

    private static void fail(String message) {
        throw new ResolutionRuleValidationException(message);
    }
}
