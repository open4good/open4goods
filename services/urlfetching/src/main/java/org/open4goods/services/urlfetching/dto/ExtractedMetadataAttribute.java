package org.open4goods.services.urlfetching.dto;

/**
 * Attribute extracted from structured HTML metadata before markdown conversion.
 *
 * @param name     normalized attribute name
 * @param value    extracted attribute value
 * @param source   metadata source, such as {@code jsonld}, {@code meta}, or {@code itemprop}
 * @param language optional language code when known
 */
public record ExtractedMetadataAttribute(String name, String value, String source, String language) {
}
