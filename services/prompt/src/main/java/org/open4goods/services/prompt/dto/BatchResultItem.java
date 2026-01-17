package org.open4goods.services.prompt.dto;

import java.util.Map;

/**
 * Provider-agnostic batch output entry.
 */
public record BatchResultItem(String customId, String content, String raw, Map<String, Object> metadata) {
}
