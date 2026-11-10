package org.open4goods.nudgerfrontapi.service.css;

import java.util.Map;
import java.util.TreeMap;

/**
 * State of the last successful sends (GOU-2081), keyed by {@code rawProvidedId} (the GTIN).
 *
 * @param entries one entry per product currently known by Google
 */
public record CssSyncState(Map<String, Entry> entries) {

    /**
     * @param fingerprint SHA-256 of the input without its {@code expirationDate}
     * @param vertical    vertical of the product when sent
     * @param sentAt      ISO instant of the last successful insert
     * @param expiresAt   ISO instant of the {@code expirationDate} of that insert
     */
    public record Entry(String fingerprint, String vertical, String sentAt, String expiresAt) {
    }

    /** @return an empty state */
    public static CssSyncState empty() {
        return new CssSyncState(new TreeMap<>());
    }
}
