package org.open4goods.services.prompt.service.provider;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

/**
 * Parser for OpenAI annotations (search-preview citations).
 */
public final class OpenAiAnnotationParser {

    private OpenAiAnnotationParser() {
    }

    /**
     * Extracts citations from a raw OpenAI response payload.
     *
     * @param objectMapper the mapper used to parse JSON
     * @param rawResponse  the raw response payload
     * @return a list of citation maps
     * @throws Exception when parsing fails
     */
    public static List<Map<String, Object>> parseCitations(ObjectMapper objectMapper, String rawResponse)
            throws Exception {
        JsonNode root = objectMapper.readTree(rawResponse);
        ArrayNode annotations = findAnnotationsNode(root);
        if (annotations == null || annotations.isEmpty()) {
            return List.of();
        }
        return mapAnnotationNodes(annotations);
    }

    /**
     * Extracts citations from annotations metadata.
     *
     * @param annotations the annotations list
     * @return a list of citation maps
     */
    public static List<Map<String, Object>> parseCitations(List<?> annotations) {
        return mapAnnotations(annotations);
    }

    private static ArrayNode findAnnotationsNode(JsonNode root) {
        JsonNode chatAnnotations = root.path("choices").path(0).path("message").path("annotations");
        if (chatAnnotations.isArray()) {
            return (ArrayNode) chatAnnotations;
        }
        JsonNode outputAnnotations = root.path("output").path(0).path("content").path(0).path("annotations");
        if (outputAnnotations.isArray()) {
            return (ArrayNode) outputAnnotations;
        }
        return null;
    }

    private static List<Map<String, Object>> mapAnnotationNodes(ArrayNode annotations) {
        List<Map<String, Object>> citations = new ArrayList<>();
        int index = 1;
        for (JsonNode annotation : annotations) {
            JsonNode urlCitation = annotation.path("url_citation");
            if (urlCitation.isMissingNode()) {
                continue;
            }
            String url = urlCitation.path("url").asText(null);
            if (!StringUtils.hasText(url)) {
                continue;
            }
            String title = urlCitation.path("title").asText(null);
            String snippet = urlCitation.path("snippet").asText(null);
            citations.add(buildCitation(index++, title, url, snippet));
        }
        return citations;
    }

    private static List<Map<String, Object>> mapAnnotations(List<?> annotations) {
        List<Map<String, Object>> citations = new ArrayList<>();
        int index = 1;
        for (Object annotation : annotations) {
            if (!(annotation instanceof Map<?, ?> map)) {
                continue;
            }
            Object urlCitation = map.get("url_citation");
            if (!(urlCitation instanceof Map<?, ?> citationMap)) {
                continue;
            }
            String url = Objects.toString(citationMap.get("url"), null);
            if (!StringUtils.hasText(url)) {
                continue;
            }
            String title = Objects.toString(citationMap.get("title"), null);
            String snippet = Objects.toString(citationMap.get("snippet"), null);
            citations.add(buildCitation(index++, title, url, snippet));
        }
        return citations;
    }

    private static Map<String, Object> buildCitation(int number, String title, String url, String snippet) {
        return Map.of(
                "number", number,
                "title", StringUtils.hasText(title) ? title : url,
                "url", url,
                "snippet", StringUtils.hasText(snippet) ? snippet : ""
        );
    }
}
