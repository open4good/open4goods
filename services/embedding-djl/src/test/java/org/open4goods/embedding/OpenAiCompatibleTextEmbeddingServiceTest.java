package org.open4goods.embedding;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.open4goods.embedding.config.DjlEmbeddingProperties;
import org.open4goods.embedding.config.DjlEmbeddingProperties.Provider;
import org.open4goods.embedding.service.OpenAiCompatibleTextEmbeddingService;

import tools.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;

class OpenAiCompatibleTextEmbeddingServiceTest
{
    private HttpServer server;

    @AfterEach
    void tearDown()
    {
        if (server != null)
        {
            server.stop(0);
        }
    }

    @Test
    void embedBatchCallsOpenAiCompatibleEndpointInInputOrder() throws IOException
    {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/v1/embeddings", exchange -> {
            assertThat(exchange.getRequestHeaders().getFirst("Authorization")).isEqualTo("Bearer test-key");
            String requestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            assertThat(requestBody).contains("\"model\":\"test-model\"");
            assertThat(requestBody).contains("\"one\"");
            assertThat(requestBody).contains("\"two\"");

            byte[] response = """
                    {
                      "data": [
                        {"index": 1, "embedding": [0.3, 0.4]},
                        {"index": 0, "embedding": [0.1, 0.2]}
                      ],
                      "model": "test-model"
                    }
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            try (OutputStream output = exchange.getResponseBody())
            {
                output.write(response);
            }
        });
        server.start();

        DjlEmbeddingProperties properties = new DjlEmbeddingProperties();
        properties.setProvider(Provider.OPENAI_COMPATIBLE);
        properties.getOpenai().setBaseUrl("http://localhost:" + server.getAddress().getPort() + "/v1");
        properties.getOpenai().setApiKey("test-key");
        properties.getOpenai().setModel("test-model");

        OpenAiCompatibleTextEmbeddingService service =
                new OpenAiCompatibleTextEmbeddingService(properties, new ObjectMapper());

        List<float[]> vectors = service.embedBatch(List.of("one", "two"));

        assertThat(vectors).hasSize(2);
        assertThat(vectors.get(0)).containsExactly(0.1f, 0.2f);
        assertThat(vectors.get(1)).containsExactly(0.3f, 0.4f);
    }
}
