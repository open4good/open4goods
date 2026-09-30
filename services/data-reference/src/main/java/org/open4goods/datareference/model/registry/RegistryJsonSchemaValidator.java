package org.open4goods.datareference.model.registry;

import java.util.Set;

import tools.jackson.databind.JsonNode;

/**
 * Structural validator for the checked-in O4G registry JSON Schema.
 *
 * <p>The source-neutral contract deliberately has no runtime dependency on a
 * provider SDK or an application framework. This validator enforces the subset
 * of JSON Schema used by the bundled schema (objects, required properties,
 * arrays, string/integer primitives and no additional properties); the typed
 * records then enforce semantic constraints such as UCUM and interval validity.
 */
final class RegistryJsonSchemaValidator {

    private RegistryJsonSchemaValidator() {
    }

    static void validate(JsonNode root) {
        object(root, "$", Set.of("schemaVersion", "registryVersion", "classes", "attributes", "verticalViews"),
                Set.of("schemaVersion", "registryVersion", "classes", "attributes", "verticalViews"));
        string(root.path("schemaVersion"), "$.schemaVersion");
        integer(root.path("registryVersion"), "$.registryVersion");
        array(root.path("classes"), "$.classes");
        array(root.path("attributes"), "$.attributes");
        array(root.path("verticalViews"), "$.verticalViews");
        for (int index = 0; index < root.path("classes").size(); index++) {
            validateClass(root.path("classes").path(index), "$.classes[" + index + "]");
        }
        for (int index = 0; index < root.path("attributes").size(); index++) {
            validateAttribute(root.path("attributes").path(index), "$.attributes[" + index + "]");
        }
        for (int index = 0; index < root.path("verticalViews").size(); index++) {
            validateVerticalView(root.path("verticalViews").path(index), "$.verticalViews[" + index + "]");
        }
    }

    private static void validateClass(JsonNode node, String path) {
        object(node, path, Set.of("id", "lifecycle", "labels", "parent", "attributes", "mappings"),
                Set.of("id", "lifecycle", "labels", "attributes", "mappings"));
        string(node.path("id"), path + ".id");
        string(node.path("lifecycle"), path + ".lifecycle");
        translations(node.path("labels"), path + ".labels");
        nullableString(node.path("parent"), path + ".parent");
        stringArray(node.path("attributes"), path + ".attributes");
        mappings(node.path("mappings"), path + ".mappings");
    }

    private static void validateAttribute(JsonNode node, String path) {
        object(node, path, Set.of("id", "lifecycle", "labels", "valueType", "cardinality", "dimension",
                "canonicalUnit", "constraints", "mappings", "resolutionPolicy"),
                Set.of("id", "lifecycle", "labels", "valueType", "cardinality", "constraints", "mappings",
                        "resolutionPolicy"));
        string(node.path("id"), path + ".id");
        string(node.path("lifecycle"), path + ".lifecycle");
        translations(node.path("labels"), path + ".labels");
        string(node.path("valueType"), path + ".valueType");
        string(node.path("cardinality"), path + ".cardinality");
        nullableString(node.path("dimension"), path + ".dimension");
        nullableString(node.path("canonicalUnit"), path + ".canonicalUnit");
        constraints(node.path("constraints"), path + ".constraints");
        mappings(node.path("mappings"), path + ".mappings");
        string(node.path("resolutionPolicy"), path + ".resolutionPolicy");
    }

    private static void validateVerticalView(JsonNode node, String path) {
        object(node, path, Set.of("verticalId", "includedClasses"), Set.of("verticalId", "includedClasses"));
        string(node.path("verticalId"), path + ".verticalId");
        stringArray(node.path("includedClasses"), path + ".includedClasses");
    }

    private static void translations(JsonNode node, String path) {
        if (!node.isObject()) {
            fail(path + " must be an object");
        }
        if (!node.hasNonNull("en") || !node.hasNonNull("fr")) {
            fail(path + " must contain English and French translations");
        }
        node.propertyStream().forEach(entry -> string(entry.getValue(), path + "." + entry.getKey()));
        string(node.path("en"), path + ".en");
        string(node.path("fr"), path + ".fr");
    }

    private static void constraints(JsonNode node, String path) {
        object(node, path, Set.of("minimum", "maximum", "allowedCodes"), Set.of("allowedCodes"));
        nullableNumber(node.path("minimum"), path + ".minimum");
        nullableNumber(node.path("maximum"), path + ".maximum");
        stringArray(node.path("allowedCodes"), path + ".allowedCodes");
    }

    private static void mappings(JsonNode node, String path) {
        array(node, path);
        for (int index = 0; index < node.size(); index++) {
            JsonNode mapping = node.path(index);
            String mappingPath = path + "[" + index + "]";
            object(mapping, mappingPath, Set.of("system", "externalId", "status", "effectiveFrom", "effectiveTo"),
                    Set.of("system", "externalId", "status", "effectiveFrom"));
            string(mapping.path("system"), mappingPath + ".system");
            string(mapping.path("externalId"), mappingPath + ".externalId");
            string(mapping.path("status"), mappingPath + ".status");
            string(mapping.path("effectiveFrom"), mappingPath + ".effectiveFrom");
            nullableString(mapping.path("effectiveTo"), mappingPath + ".effectiveTo");
        }
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
        if (!node.isNull()) {
            string(node, path);
        }
    }

    private static void integer(JsonNode node, String path) {
        if (!node.isIntegralNumber()) {
            fail(path + " must be an integer");
        }
    }

    private static void nullableNumber(JsonNode node, String path) {
        if (!node.isNull() && !node.isNumber()) {
            fail(path + " must be a number or null");
        }
    }

    private static void fail(String message) {
        throw new RegistryValidationException(message);
    }
}
