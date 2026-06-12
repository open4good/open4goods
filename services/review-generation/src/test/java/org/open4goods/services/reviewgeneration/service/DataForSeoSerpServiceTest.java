package org.open4goods.services.reviewgeneration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;
import org.open4goods.model.attribute.ReferentielKey;
import org.open4goods.model.product.Product;
import org.open4goods.services.productrepository.services.ProductRepository;
import org.open4goods.services.reviewgeneration.config.DataForSeoSerpConfig;
import org.open4goods.services.reviewgeneration.config.ReviewGenerationConfig;
import org.open4goods.services.reviewgeneration.dto.SourceDiscoveryJob;
import org.open4goods.verticals.VerticalsConfigService;

class DataForSeoSerpServiceTest {

    @TempDir
    private Path tempDir;

    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void discoverUrlsForVertical_ShouldSubmitDepthTenBrandModelQueriesInChunksOfOneHundred() throws Exception {
        List<String> payloads = new ArrayList<>();
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/v3/serp/google/organic/task_post", exchange -> {
            payloads.add(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            String response = """
                    {"tasks":[{"id":"task-1","status_code":20100}]}
                    """;
            exchange.sendResponseHeaders(200, response.getBytes(StandardCharsets.UTF_8).length);
            try (OutputStream body = exchange.getResponseBody()) {
                body.write(response.getBytes(StandardCharsets.UTF_8));
            }
        });
        server.start();

        ProductRepository productRepository = Mockito.mock(ProductRepository.class);
        when(productRepository.exportVerticalWithValidDateOrderByImpactScore("tv", 101, false))
                .thenReturn(products(101));

        DataForSeoSerpService service = new DataForSeoSerpService(config(), reviewConfig(),
                productRepository, Mockito.mock(VerticalsConfigService.class));

        SourceDiscoveryJob job = service.discoverUrlsForVertical("tv", 101, true);

        assertThat(job.getSubmittedTasks()).isEqualTo(101);
        assertThat(payloads).hasSize(2);
        assertThat(payloads.getFirst()).contains("\"depth\":10");
        assertThat(payloads.getFirst()).contains("Brand0");
        assertThat(payloads.getFirst()).contains("\\\"Model0\\\"");
        assertThat(payloads.getFirst()).contains("fiche technique");
    }

    private DataForSeoSerpConfig config() {
        DataForSeoSerpConfig config = new DataForSeoSerpConfig();
        config.setUsername("user");
        config.setPassword("password");
        config.setBaseUrl("http://localhost:" + server.getAddress().getPort());
        config.setDepth(10);
        return config;
    }

    private ReviewGenerationConfig reviewConfig() {
        ReviewGenerationConfig config = new ReviewGenerationConfig();
        config.setBatchFolder(tempDir.toString());
        return config;
    }

    private Stream<Product> products(int count) {
        List<Product> products = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Product product = new Product();
            product.setId((long) i);
            product.setVertical("tv");
            product.getAttributes().getReferentielAttributes().put(ReferentielKey.BRAND, "Brand" + i);
            product.getAttributes().getReferentielAttributes().put(ReferentielKey.MODEL, "Model" + i);
            products.add(product);
        }
        return products.stream();
    }
}
