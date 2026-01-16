package org.open4goods.services.prompt.service.provider;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Tests for OpenAI annotations parsing.
 */
class OpenAiAnnotationParserTest {

    @Test
    void shouldParseSearchPreviewAnnotationsFromChatCompletion() throws Exception {
        String raw = """
                {
                  "id": "chatcmpl-test",
                  "choices": [
                    {
                      "message": {
                        "content": "{\\"description\\":\\"ok\\"}",
                        "annotations": [
                          {
                            "type": "url_citation",
                            "url_citation": {
                              "url": "https://example.com/review",
                              "title": "Example Review",
                              "snippet": "Snippet text"
                            }
                          }
                        ]
                      }
                    }
                  ],
                  "extra_field": "ignored"
                }
                """;

        List<Map<String, Object>> citations = OpenAiAnnotationParser.parseCitations(new ObjectMapper(), raw);

        assertThat(citations).hasSize(1);
        assertThat(citations.get(0)).containsEntry("url", "https://example.com/review");
        assertThat(citations.get(0)).containsEntry("title", "Example Review");
    }
}
