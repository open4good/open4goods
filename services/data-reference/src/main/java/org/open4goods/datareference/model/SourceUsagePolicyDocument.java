package org.open4goods.datareference.model;

import java.util.List;
import java.util.Objects;

/**
 * Git-authored collection of versioned source-usage policy records.
 *
 * @param schemaVersion fixed JSON contract identifier
 * @param policies immutable policy records
 */
public record SourceUsagePolicyDocument(String schemaVersion, List<SourceUsagePolicy> policies) {

    /** Current schema identifier for source-usage policy resources. */
    public static final String SCHEMA_VERSION = "https://open4goods.org/schema/source-usage-policy-1.json";

    /**
     * Validates the contract version and policy list.
     */
    public SourceUsagePolicyDocument {
        if (!SCHEMA_VERSION.equals(schemaVersion)) {
            throw new IllegalArgumentException("Unsupported source usage policy schema: " + schemaVersion);
        }
        policies = List.copyOf(Objects.requireNonNull(policies, "policies must not be null"));
        if (policies.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("policies must not contain null");
        }
    }
}
