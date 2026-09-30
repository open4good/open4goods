package org.open4goods.datareference.model.registry;

import java.time.Instant;
import java.util.Objects;

/**
 * Reconciliation evidence produced when a Git registry version reaches the read alias.
 *
 * @param alias stable read alias switched to the indexed registry
 * @param registryVersion installed semantic registry version
 * @param contentHash SHA-256 of the exact authored bytes
 * @param classCount installed class count
 * @param attributeCount installed attribute count
 * @param installedAt installation instant
 * @param idempotent whether the already-installed exact revision was retained
 */
public record RegistryImportReport(
        String alias,
        RegistryVersion registryVersion,
        String contentHash,
        int classCount,
        int attributeCount,
        Instant installedAt,
        boolean idempotent) {

    /** Validates the evidence an operator needs to reconcile an import. */
    public RegistryImportReport {
        if (alias == null || alias.isBlank()) {
            throw new IllegalArgumentException("alias must not be blank");
        }
        Objects.requireNonNull(registryVersion, "registryVersion must not be null");
        if (contentHash == null || !contentHash.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("contentHash must be a lower-case SHA-256 digest");
        }
        if (classCount < 0 || attributeCount < 0) {
            throw new IllegalArgumentException("indexed counts must not be negative");
        }
        Objects.requireNonNull(installedAt, "installedAt must not be null");
    }
}
