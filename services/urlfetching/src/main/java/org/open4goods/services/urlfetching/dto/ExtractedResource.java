package org.open4goods.services.urlfetching.dto;

/**
 * External product resource discovered on an official brand page.
 *
 * @param url absolute resource URL
 * @param type resource media type
 * @param source extraction source
 * @param label optional human-readable label
 */
public record ExtractedResource(String url, ResourceType type, String source, String label) {
}
