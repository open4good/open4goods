package org.open4goods.datareference.model.grouping;

import java.util.Set;

import tools.jackson.databind.JsonNode;

/**
 * Structural validator for the checked-in model-pattern-rule JSON Schema.
 */
final class ModelPatternRuleJsonSchemaValidator {

    private ModelPatternRuleJsonSchemaValidator() {
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
        object(node, path,
                Set.of("version", "canonicalBrand", "canonicalClass", "pattern", "examples", "counterexamples", "reviewer"),
                Set.of("version", "canonicalBrand", "canonicalClass", "pattern", "examples", "counterexamples", "reviewer"));
        string(node.path("version"), path + ".version");
        string(node.path("canonicalBrand"), path + ".canonicalBrand");
        string(node.path("canonicalClass"), path + ".canonicalClass");
        string(node.path("pattern"), path + ".pattern");
        stringArray(node.path("examples"), path + ".examples");
        if (node.path("examples").isEmpty()) {
            fail(path + ".examples must not be empty");
        }
        stringArray(node.path("counterexamples"), path + ".counterexamples");
        string(node.path("reviewer"), path + ".reviewer");
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

    private static void fail(String message) {
        throw new ModelPatternRuleValidationException(message);
    }
}
