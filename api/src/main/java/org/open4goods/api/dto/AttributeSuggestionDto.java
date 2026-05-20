package org.open4goods.api.dto;

import java.util.List;
import java.util.Map;

/**
 * A single attribute suggestion for a vertical, annotated with coverage stats and a flag
 * indicating whether the attribute YAML definition file already exists on the classpath.
 * Intended for agents building the {@code attributesConfig.configs} section of a
 * {@code VerticalConfig} YAML without server-side file mutations.
 */
public record AttributeSuggestionDto(
        String key,
        int hits,
        int coveragePercent,
        boolean ymlExists,
        List<String> datasources,
        Map<String, Integer> topValues
) {}
