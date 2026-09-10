package org.open4goods.datareference.model;

/**
 * Immutable reference from a snapshot to the policy used when it was retrieved.
 *
 * @param policyId stable policy identifier
 * @param version policy version
 */
public record SourceUsagePolicyRef(String policyId, String version) {

    /**
     * Validates the policy reference.
     */
    public SourceUsagePolicyRef {
        if (policyId == null || policyId.isBlank() || version == null || version.isBlank()) {
            throw new IllegalArgumentException("policy id and version must not be blank");
        }
        policyId = policyId.trim();
        version = version.trim();
    }
}
