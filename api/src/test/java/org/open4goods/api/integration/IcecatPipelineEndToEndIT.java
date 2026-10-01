package org.open4goods.api.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.open4goods.api.services.completion.IcecatCompletionService;
import org.open4goods.commons.services.AbstractCompletionService;
import org.open4goods.datareference.model.Gtin;
import org.open4goods.datareference.model.ProjectionSurface;
import org.open4goods.datareference.model.RuleVersion;
import org.open4goods.datareference.model.SourceRecordHead;
import org.open4goods.datareference.model.SourceRecordKey;
import org.open4goods.datareference.model.SourceRecordMutation;
import org.open4goods.datareference.model.SourceRecordTransition;
import org.open4goods.datareference.model.SourceRecordTransitionOutcome;
import org.open4goods.datareference.model.SourceUsagePolicyRegistry;
import org.open4goods.datareference.model.projection.EvaluationRefreshTrigger;
import org.open4goods.datareference.model.projection.EvaluationSummary;
import org.open4goods.datareference.model.projection.OfferSummary;
import org.open4goods.datareference.model.projection.ProductReferenceProjection;
import org.open4goods.datareference.model.projection.ProductReferenceProjectionEnvelope;
import org.open4goods.datareference.model.projection.ProjectionReplayInputs;
import org.open4goods.datareference.model.projection.SearchSummary;
import org.open4goods.datareference.model.registry.GitRegistryLoader;
import org.open4goods.datareference.model.registry.InMemoryCanonicalRegistry;
import org.open4goods.datareference.model.registry.RegistryVersion;
import org.open4goods.datareference.model.resolution.GitResolutionRuleLoader;
import org.open4goods.datareference.model.resolution.ResolutionRuleRegistry;
import org.open4goods.datareference.model.value.QuantityValue;
import org.open4goods.datareference.port.CorrectionsPort;
import org.open4goods.datareference.port.IngestionCheckpointStore;
import org.open4goods.datareference.port.ProjectionWritePort;
import org.open4goods.datareference.port.ScanFailure;
import org.open4goods.datareference.port.ScanPage;
import org.open4goods.datareference.port.ScanRequest;
import org.open4goods.datareference.port.SourceRecordHeadStore;
import org.open4goods.datareference.service.DeterministicDomainSliceComposer;
import org.open4goods.datareference.service.DeterministicResolutionService;
import org.open4goods.datareference.service.ProjectionAssemblyService;
import org.open4goods.datareference.service.SharedNormalizationService;
import org.open4goods.icecat.config.yml.IcecatCompletionConfig;
import org.open4goods.icecat.model.IcecatCatalogueCategory;
import org.open4goods.icecat.model.IcecatCategoryDocument;
import org.open4goods.icecat.services.IcecatCategoryVerticalResolver;
import org.open4goods.icecat.services.IcecatIndexVersionManager;
import org.open4goods.icecat.services.IcecatIndexVersionManager.IndexSwitchResult;
import org.open4goods.icecat.services.IcecatLiveClient;
import org.open4goods.icecat.services.IcecatRegistryProjectionService;
import org.open4goods.icecat.services.IcecatSourceRecordAdapter;
import org.open4goods.icecat.services.loader.IcecatReferenceCatalogueReader;
import org.open4goods.model.product.Product;
import org.open4goods.model.vertical.VerticalConfig;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchClients;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.client.elc.rest5_client.Rest5Clients;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.sun.net.httpserver.HttpServer;

/**
 * GOU-186 (AC1 of GOU-39): one production-shaped fixture chaining the whole Icecat pipeline --
 * generated bulk-export XML, a versioned/aliased Elasticsearch index, a live-API lookup, the
 * reviewed category-to-vertical mapping, unit normalization, deterministic surface resolution and
 * domain-slice assembly -- terminating in a {@link ProductReferenceProjection}, asserting each
 * stage's output feeds the next, with no direct {@code Product}/{@code ProductRepository}
 * mutation anywhere in the chain.
 */
@Testcontainers
class IcecatPipelineEndToEndIT {

    private static final int ICECAT_CATEGORY_ID = 1584;
    private static final int ICECAT_PRODUCT_ID = 158401;
    private static final String GTIN_VALUE = "4006381333931";

    @Container
    static final ElasticsearchContainer ELASTICSEARCH = new ElasticsearchContainer(
            DockerImageName.parse("docker.elastic.co/elasticsearch/elasticsearch:9.5.1"))
            .withEnv("xpack.security.enabled", "false")
            .withEnv("discovery.type", "single-node");

    static ElasticsearchOperations operations;

    @BeforeAll
    static void connect() throws Exception {
        ClientConfiguration clientConfiguration = ClientConfiguration.builder()
                .connectedTo(ELASTICSEARCH.getHttpHostAddress())
                .build();
        ElasticsearchClient client = ElasticsearchClients.createImperative(Rest5Clients.getRest5Client(clientConfiguration));
        operations = new ElasticsearchTemplate(client);

        // Allow the wildcard index delete this test class uses for cleanup; ES blocks it by
        // default (action.destructive_requires_name) even on this throwaway container.
        java.net.http.HttpClient.newHttpClient().send(java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create("http://" + ELASTICSEARCH.getHttpHostAddress() + "/_cluster/settings"))
                .header("Content-Type", "application/json")
                .PUT(java.net.http.HttpRequest.BodyPublishers.ofString(
                        "{\"persistent\":{\"action.destructive_requires_name\":false}}"))
                .build(), java.net.http.HttpResponse.BodyHandlers.discarding());
    }

    @AfterAll
    static void cleanUp() {
        operations.indexOps(IndexCoordinates.of("icecat-categories*")).delete();
    }

    @Test
    void xmlCategoryFlowsThroughIndexLiveLookupMappingNormalizationToAProjection() throws Exception {
        // Stage 1: parse a production-shaped bulk-export categories fixture (no network, no mocks).
        List<IcecatCatalogueCategory> categories;
        try (InputStream in = getClass().getResourceAsStream("/icecat/inventory/CategoriesList-sample.xml")) {
            categories = IcecatReferenceCatalogueReader.readCategories(in, "CategoriesList");
        }
        IcecatCatalogueCategory television = categories.stream()
                .filter(category -> category.id() == ICECAT_CATEGORY_ID).findFirst().orElseThrow();
        assertThat(television.englishName()).isEqualTo("Televisions");

        // Stage 2: version that parsed category into a real, aliased Elasticsearch index.
        IcecatIndexVersionManager indexManager = new IcecatIndexVersionManager(operations);
        IcecatCategoryDocument categoryDocument = new IcecatCategoryDocument();
        categoryDocument.setId(television.id());
        categoryDocument.setEnglishName(television.englishName());
        categoryDocument.setParentId(television.parentId());
        categoryDocument.setScore(television.score());
        IndexSwitchResult indexResult = indexManager.reimport(IcecatCategoryDocument.class, List.of(categoryDocument));
        String categoryAlias = operations.indexOps(IcecatCategoryDocument.class).getIndexCoordinates().getIndexName();

        assertThat(indexResult.documentCount()).isEqualTo(1);
        assertThat(indexManager.currentIndexesForAlias(categoryAlias)).containsExactly(indexResult.indexName());
        assertThat(operations.count(org.springframework.data.elasticsearch.core.query.Query.findAll(),
                IcecatCategoryDocument.class, IndexCoordinates.of(categoryAlias))).isEqualTo(1);

        // Stage 3: live-API lookup, translated into a source-neutral head through the real
        // production completion service boundary -- never a Product/ProductRepository write.
        HttpServer liveServer = startLiveServer();
        try {
            IcecatCompletionConfig config = new IcecatCompletionConfig();
            config.setIceCatUrlPrefix("http://localhost:" + liveServer.getAddress().getPort() + "/api?UserName=test&GTIN=");
            config.setPolitenessDelayMs(0);
            IcecatLiveClient liveClient = new IcecatLiveClient(config);
            IcecatSourceRecordAdapter adapter = new IcecatSourceRecordAdapter();
            InMemorySourceRecordHeadStore sourceRecordStore = new InMemorySourceRecordHeadStore();
            IngestionCheckpointStore checkpointStore = Mockito.mock(IngestionCheckpointStore.class);
            Mockito.when(checkpointStore.find(ArgumentMatchers.anyString(), ArgumentMatchers.any()))
                    .thenReturn(Optional.empty());

            IcecatCompletionService completionService = Mockito.mock(IcecatCompletionService.class, Mockito.CALLS_REAL_METHODS);
            inject(IcecatCompletionService.class, completionService, "icecatConfig", config);
            inject(IcecatCompletionService.class, completionService, "liveClient", liveClient);
            inject(IcecatCompletionService.class, completionService, "adapter", adapter);
            inject(IcecatCompletionService.class, completionService, "sourceRecordStore", sourceRecordStore);
            inject(IcecatCompletionService.class, completionService, "checkpointStore", checkpointStore);
            inject(AbstractCompletionService.class, completionService, "logger",
                    org.slf4j.LoggerFactory.getLogger(IcecatPipelineEndToEndIT.class));

            Gtin gtin = new Gtin(GTIN_VALUE);
            Product product = new Product(Long.parseLong(GTIN_VALUE));

            completionService.processProduct(new VerticalConfig(), product);

            assertThat(product.getExternalIds().getIcecat()).isEqualTo(String.valueOf(ICECAT_PRODUCT_ID));
            SourceRecordKey recordKey = IcecatSourceRecordAdapter.keyFor(ICECAT_PRODUCT_ID);
            SourceRecordHead head = sourceRecordStore.find(recordKey).orElseThrow();
            assertThat(head.gtinLinks()).anySatisfy(link -> assertThat(link.gtin()).isEqualTo(gtin));
            assertThat(head.assertions()).anySatisfy(assertion -> assertThat(assertion.field().key()).isEqualTo("feature:1649"));

            // Stage 4: reviewed category -> vertical mapping, over the very category this fixture parsed.
            IcecatCategoryVerticalResolver verticalResolver =
                    new IcecatCategoryVerticalResolver(new IcecatRegistryProjectionService());
            assertThat(verticalResolver.resolveVerticalId(television.id(), LocalDate.of(2026, 9, 12))).hasValue("tv");

            // Stage 5+6: real normalization, deterministic resolution and domain-slice assembly,
            // reading only from the source-record head produced in stage 3.
            InMemoryCanonicalRegistry registry = new GitRegistryLoader().loadDefault().registry();
            SharedNormalizationService normalizer = new SharedNormalizationService(registry, Map.of());
            SourceUsagePolicyRegistry policies = SourceUsagePolicyRegistry.loadDefault();
            ResolutionRuleRegistry rules = new GitResolutionRuleLoader(registry, policies).loadDefault();
            DeterministicResolutionService resolution =
                    new DeterministicResolutionService(policies, normalizer, noCorrections(), rules);

            CapturingWriter writer = new CapturingWriter();
            ProjectionAssemblyService assembly = new ProjectionAssemblyService(sourceRecordStore, resolution,
                    (g, surface, inputs) -> new OfferSummary(0, false, null, null, Instant.now()),
                    input -> new EvaluationSummary(new RuleVersion("evaluation", 1), Instant.now(), Map.of(), Map.of(), List.of()),
                    (g, surface, inputs, values) -> new SearchSummary(new RuleVersion("lexical-search", 1), List.of()),
                    new DeterministicDomainSliceComposer(), writer, Clock.systemUTC());

            ProjectionReplayInputs replayInputs = new ProjectionReplayInputs(new RegistryVersion(1),
                    new RuleVersion("normalization", 1), new RuleVersion("resolution", 1), List.of(), Instant.now());

            ProductReferenceProjectionEnvelope envelope =
                    assembly.rebuild(gtin, replayInputs, EvaluationRefreshTrigger.REFERENCE_CHANGED);

            // Stage 7: the terminus -- a ProductReferenceProjection, written through the typed
            // projection port only, never through Product/ProductRepository.
            assertThat(writer.written).isEqualTo(envelope);
            ProductReferenceProjection webProjection = envelope.components().get(ProjectionSurface.NUDGER_WEB);
            assertThat(webProjection.gtin()).isEqualTo(gtin);
            assertThat(webProjection.resolvedValues()).anySatisfy(value -> {
                assertThat(value.attribute().slug()).isEqualTo("width");
                assertThat(((QuantityValue) value.value()).amount()).isEqualByComparingTo(new BigDecimal("125.00"));
            });
        } finally {
            liveServer.stop(0);
        }
    }

    private static HttpServer startLiveServer() throws IOException {
        String body = """
                {
                  "msg": "OK",
                  "data": {
                    "GeneralInfo": {
                      "IcecatId": %d,
                      "Title": "Example OLED Television",
                      "Brand": "Acme",
                      "ProductName": "OLED-55",
                      "GTIN": ["%s"],
                      "Category": {
                        "CategoryID": "%d",
                        "Name": { "Value": "Televisions", "Language": "en" }
                      }
                    },
                    "FeaturesGroups": [
                      {
                        "ID": "1",
                        "Features": [
                          {
                            "ID": "1649",
                            "CategoryFeatureId": "1649",
                            "Localized": "0",
                            "RawValue": "1.25",
                            "Value": "1.25",
                            "Feature": {
                              "ID": "1649",
                              "Sign": "m",
                              "Name": { "Value": "Width" }
                            }
                          }
                        ]
                      }
                    ]
                  }
                }
                """.formatted(ICECAT_PRODUCT_ID, GTIN_VALUE, ICECAT_CATEGORY_ID);

        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/api", exchange -> {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(bytes);
            }
        });
        server.start();
        return server;
    }

    private static void inject(Class<?> declaringClass, Object target, String fieldName, Object value) throws Exception {
        Field field = declaringClass.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static CorrectionsPort noCorrections() {
        return new CorrectionsPort() {
            @Override
            public List<org.open4goods.datareference.model.resolution.Correction> findByGtin(Gtin gtin) {
                return List.of();
            }

            @Override
            public ScanPage<org.open4goods.datareference.model.resolution.Correction> scanAll(ScanRequest request) {
                return ScanPage.last(List.of());
            }
        };
    }

    /** Minimal in-memory {@link SourceRecordHeadStore}: CAS by key, no journal retained. */
    private static final class InMemorySourceRecordHeadStore implements SourceRecordHeadStore {
        private final Map<SourceRecordKey, SourceRecordHead> heads = new ConcurrentHashMap<>();
        private final Map<SourceRecordKey, Long> revisions = new ConcurrentHashMap<>();

        @Override
        public SourceRecordTransition apply(SourceRecordMutation mutation) {
            SourceRecordHead candidate = mutation.candidate();
            SourceRecordHead current = heads.get(candidate.key());
            long revision = revisions.merge(candidate.key(), 1L, Long::sum);
            SourceRecordTransitionOutcome outcome;
            if (candidate.supersedes(current)) {
                heads.put(candidate.key(), candidate);
                outcome = SourceRecordTransitionOutcome.ACCEPTED;
            } else {
                outcome = SourceRecordTransitionOutcome.OUT_OF_ORDER;
            }
            return new SourceRecordTransition(SourceRecordTransition.idFor(candidate.key(), revision), candidate.key(),
                    revision, candidate.schemaVersion(), candidate.providerVersion(),
                    current == null ? null : current.payloadHash(), candidate.payloadHash(), candidate.observedAt(),
                    candidate.retrievedAt(), candidate.state(), outcome, null, List.of());
        }

        @Override
        public Optional<SourceRecordHead> find(SourceRecordKey key) {
            return Optional.ofNullable(heads.get(key));
        }

        @Override
        public List<SourceRecordHead> findByGtin(Gtin gtin) {
            List<SourceRecordHead> result = new ArrayList<>();
            for (SourceRecordHead head : heads.values()) {
                if (head.gtinLinks().stream().anyMatch(link -> link.gtin().equals(gtin))) {
                    result.add(head);
                }
            }
            return result;
        }

        @Override
        public boolean storeIfNewer(SourceRecordHead head) {
            SourceRecordHead current = heads.get(head.key());
            if (head.supersedes(current)) {
                heads.put(head.key(), head);
                return true;
            }
            return false;
        }

        @Override
        public boolean delete(SourceRecordKey key) {
            return heads.remove(key) != null;
        }
    }

    private static final class CapturingWriter implements ProjectionWritePort {
        private ProductReferenceProjectionEnvelope written;

        @Override
        public void write(ProductReferenceProjectionEnvelope projection) {
            written = projection;
        }

        @Override
        public List<ScanFailure> writeAll(List<ProductReferenceProjectionEnvelope> projections) {
            projections.forEach(this::write);
            return List.of();
        }
    }
}
