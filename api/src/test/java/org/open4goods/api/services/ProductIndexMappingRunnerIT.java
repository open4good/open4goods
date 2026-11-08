package org.open4goods.api.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.InstanceOfAssertFactories.MAP;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.open4goods.api.services.ProductIndexMappingRunner.Action;
import org.open4goods.model.product.Product;
import org.open4goods.services.productrepository.repository.ElasticProductRepository;
import org.slf4j.LoggerFactory;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchClients;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.client.elc.rest5_client.Rest5Clients;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchCustomConversions;
import org.springframework.data.elasticsearch.core.convert.MappingElasticsearchConverter;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.mapping.SimpleElasticsearchMappingContext;
import org.springframework.data.elasticsearch.repository.support.ElasticsearchRepositoryFactory;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import ch.qos.logback.core.read.ListAppender;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch.indices.IndicesBlockOptions;

/**
 * GOU-1835 acceptance criteria against a real Elasticsearch:
 * <ul>
 * <li>C1: with {@code index.blocks.metadata=true} (or {@code read_only=true}) on {@code products-moustik}, so that
 * {@code PUT _mapping} fails, {@link ElasticProductRepository} (the only products index access ui builds at startup)
 * is still created;</li>
 * <li>C2: in the same case, the api step logs three WARN lines with the full stack trace, then one ERROR, and
 * returns;</li>
 * <li>C3: on an empty Elasticsearch, the step creates {@code products-moustik} with the same mapping and settings as
 * the former repository constructor ({@code createWithMapping()} on {@link Product}).</li>
 * </ul>
 * Not picked up by surefire (IT suffix) and no failsafe execution is bound, so it is not part of the PR CI. Run it
 * with {@code mvn -pl api test -Dtest=ProductIndexMappingRunnerIT -Dsurefire.failIfNoSpecifiedTests=false} (rootless
 * Docker required).
 */
@Testcontainers
class ProductIndexMappingRunnerIT {

    @Container
    static final ElasticsearchContainer ELASTICSEARCH = new ElasticsearchContainer(
            DockerImageName.parse("docker.elastic.co/elasticsearch/elasticsearch:9.5.1"))
            .withEnv("xpack.security.enabled", "false")
            .withEnv("discovery.type", "single-node");

    private static final String REFERENCE_INDEX = "products-reference";
    /** Settings Elasticsearch generates per index, which cannot match between two indices. */
    private static final List<String> PER_INDEX_SETTINGS = List.of("index.uuid", "index.creation_date",
            "index.provided_name", "index.version.created");
    private static final List<IndicesBlockOptions> BLOCKS = List.of(IndicesBlockOptions.Metadata,
            IndicesBlockOptions.ReadOnly);

    static ElasticsearchClient client;
    static ElasticsearchOperations operations;

    private final Logger logger = (Logger) LoggerFactory.getLogger(ProductIndexMappingRunner.class);
    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();
    private Level previousLevel;

    @BeforeAll
    static void connect() {
        ClientConfiguration clientConfiguration = ClientConfiguration.builder()
                .connectedTo(ELASTICSEARCH.getHttpHostAddress())
                .build();
        client = ElasticsearchClients.createImperative(Rest5Clients.getRest5Client(clientConfiguration));
        // The converter as Spring Boot builds it: BigDecimal and the other store types are simple types.
        ElasticsearchCustomConversions conversions = new ElasticsearchCustomConversions(List.of());
        SimpleElasticsearchMappingContext mappingContext = new SimpleElasticsearchMappingContext();
        mappingContext.setSimpleTypeHolder(conversions.getSimpleTypeHolder());
        MappingElasticsearchConverter converter = new MappingElasticsearchConverter(mappingContext);
        converter.setConversions(conversions);
        converter.afterPropertiesSet();
        operations = new ElasticsearchTemplate(client, converter);
    }

    @BeforeEach
    void captureLogs() {
        previousLevel = logger.getLevel();
        logger.setLevel(Level.INFO);
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void cleanUp() throws IOException {
        logger.detachAppender(appender);
        logger.setLevel(previousLevel);
        for (String index : List.of(Product.DEFAULT_REPO, REFERENCE_INDEX)) {
            // The metadata block refuses HEAD, DELETE and even PUT _settings on the index: remove the blocks through
            // the dedicated block API first.
            for (IndicesBlockOptions block : BLOCKS) {
                try {
                    client.indices().removeBlock(r -> r.index(index).block(block));
                } catch (ElasticsearchException e) {
                    assertThat(e.status()).as("index absent").isEqualTo(404);
                }
            }
            client.indices().delete(r -> r.index(index).ignoreUnavailable(true));
        }
    }

    @ParameterizedTest
    @EnumSource(names = { "Metadata", "ReadOnly" })
    void c1_productRepositoryIsBuiltWhenPutMappingFails(IndicesBlockOptions block) throws IOException {
        blockedProductsIndex(block);
        // Sanity: the former constructor call fails on this index.
        assertThatThrownBy(() -> operations.indexOps(Product.class).putMapping()).isInstanceOf(RuntimeException.class);

        ElasticProductRepository repository = new ElasticsearchRepositoryFactory(operations)
                .getRepository(ElasticProductRepository.class);

        assertThat(repository).isNotNull();
    }

    /**
     * {@code index.blocks.metadata=true} already refuses the {@code HEAD} of {@code exists()} (403);
     * {@code index.blocks.read_only=true} lets it through and refuses the {@code PUT _mapping} itself.
     */
    @ParameterizedTest
    @CsvSource({ "Metadata, status: 403", "ReadOnly, cluster_block_exception" })
    void c2_apiStepLogsThreeFullWarnsThenAnErrorAndReturns(IndicesBlockOptions block, String failure)
            throws IOException {
        blockedProductsIndex(block);
        ProductIndexMappingRunner runner = new ProductIndexMappingRunner(operations, Duration.ofMillis(100));

        assertThatCode(() -> assertThat(runner.applyMapping()).isEmpty()).doesNotThrowAnyException();

        List<ILoggingEvent> warns = events(Level.WARN);
        assertThat(warns).hasSize(ProductIndexMappingRunner.MAX_ATTEMPTS);
        assertThat(warns).allSatisfy(warn -> assertThat(ThrowableProxyUtil.asString(warn.getThrowableProxy()))
                .contains(failure).contains("Caused by: "));
        assertThat(events(Level.ERROR)).singleElement().extracting(ILoggingEvent::getFormattedMessage).asString()
                .contains("failed after 3 attempts").contains("the api keeps starting");
        assertThat(appender.list.getLast().getLevel()).isEqualTo(Level.ERROR);
    }

    @Test
    void c3_emptyElasticsearch_getsTheFormerSettingsAndMapping() {
        IndexOperations productOperations = operations.indexOps(Product.class);
        IndexOperations referenceOperations = operations.indexOps(IndexCoordinates.of(REFERENCE_INDEX));
        // What the former repository constructor did through createWithMapping(), under another index name.
        referenceOperations.create(productOperations.createSettings(), productOperations.createMapping());

        assertThat(new ProductIndexMappingRunner(operations, Duration.ZERO).applyMapping()).contains(Action.CREATED);

        Map<String, Object> mapping = productOperations.getMapping();
        assertThat(mapping).isEqualTo(referenceOperations.getMapping());
        assertThat(mapping).extractingByKey("properties").asInstanceOf(MAP).containsKeys("price", "vertical");
        assertThat(comparableSettings(productOperations)).isEqualTo(comparableSettings(referenceOperations))
                .containsEntry("index.number_of_shards", "10").containsEntry("index.refresh_interval", "80s")
                .containsEntry("index.analysis.analyzer.french.type", "custom");
        assertThat(events(Level.INFO)).singleElement().extracting(ILoggingEvent::getFormattedMessage).asString()
                .contains("index products-moustik, action CREATED, attempt 1/3");

        // Second startup: the index exists, its mapping is put again.
        assertThat(new ProductIndexMappingRunner(operations, Duration.ZERO).applyMapping())
                .contains(Action.MAPPING_UPDATED);
        assertThat(productOperations.getMapping()).isEqualTo(mapping);
    }

    /** Creates {@code products-moustik} and sets {@code index.blocks.<block>=true} on it. */
    private static void blockedProductsIndex(IndicesBlockOptions block) throws IOException {
        operations.indexOps(Product.class).createWithMapping();
        client.indices().addBlock(r -> r.index(Product.DEFAULT_REPO).block(block));
    }

    private static Map<String, Object> comparableSettings(IndexOperations indexOperations) {
        Map<String, Object> settings = new TreeMap<>(indexOperations.getSettings(true).flatten());
        settings.keySet().removeIf(key -> PER_INDEX_SETTINGS.stream().anyMatch(key::startsWith));
        return settings;
    }

    private List<ILoggingEvent> events(Level level) {
        return appender.list.stream().filter(event -> event.getLevel() == level).toList();
    }
}
