package org.open4goods.services.favicon.dto;

/**
 * Immutable Data Transfer Object for favicon response.
 */
public record FaviconResponse(byte[] faviconData, String contentType) { }
