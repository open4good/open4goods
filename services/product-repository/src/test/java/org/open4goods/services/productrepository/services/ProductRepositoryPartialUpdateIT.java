package org.open4goods.services.productrepository.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.MAP;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.open4goods.model.price.AggregatedPrice;
import org.open4goods.model.price.AggregatedPrices;
import org.open4goods.model.price.Currency;
import org.open4goods.model.product.Product;
import org.open4goods.model.product.ProductCondition;
import org.open4goods.model.product.ProductPartialUpdateField;
import org.open4goods.model.product.ProductPartialUpdateHolder;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchClients;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.client.elc.rest5_client.Rest5Clients;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.query.UpdateQuery;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import co.elastic.clients.json.JsonpMapper;
import co.elastic.clients.json.jackson.Jackson3JsonpMapper;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;

/**
 * GOU-1784: runs {@link ProductRepository#bulkUpdateDocument(java.util.Collection)} against a
 * real Elasticsearch with the production {@code product-mappings.json}, through the JSON mappers
 * Spring Boot may give the client ({@link Jackson3JsonpMapper} in production). Both omit null
 * properties. Not picked up by surefire (IT suffix) and no failsafe execution is bound, so it is
 * not part of the PR CI. Run it with {@code mvn -pl services/product-repository test
 * -Dtest=ProductRepositoryPartialUpdateIT -DargLine="--add-opens java.base/java.math=ALL-UNNAMED
 * --add-opens java.base/java.util=ALL-UNNAMED"} (rootless Docker required).
 */
@Testcontainers
class ProductRepositoryPartialUpdateIT {

    @Container
    static final ElasticsearchContainer ELASTICSEARCH = new ElasticsearchContainer(
            DockerImageName.parse("docker.elastic.co/elasticsearch/elasticsearch:9.5.1"))
            .withEnv("xpack.security.enabled", "false")
            .withEnv("discovery.type", "single-node");

    private static final long EXPIRED = 1_762_130_940_000L; // 2025-11-03T00:49Z, the 1 EUR offer
    private static final long NOW = 1_762_471_440_000L;

    private static long nextId = 1;

    @BeforeAll
    static void createIndex() {
        operations(new JacksonJsonpMapper()).indexOps(Product.class).createWithMapping();
    }

    static Stream<Arguments> mappers() {
        return Stream.of(
                Arguments.of("jackson3", new Jackson3JsonpMapper()),
                Arguments.of("jackson2", new JacksonJsonpMapper()));
    }

    private static ElasticsearchOperations operations(JsonpMapper mapper) {
        ClientConfiguration clientConfiguration = ClientConfiguration.builder()
                .connectedTo(ELASTICSEARCH.getHttpHostAddress())
                .build();
        return new ElasticsearchTemplate(ElasticsearchClients.createImperative(
                Rest5Clients.getRest5Client(clientConfiguration), null, mapper));
    }

    private static AggregatedPrice offer(String datasource, double price, long timestamp, Integer quantityInStock) {
        AggregatedPrice offer = new AggregatedPrice(price, Currency.EUR);
        offer.setTimeStamp(timestamp);
        offer.setDatasourceName(datasource);
        offer.setProductState(ProductCondition.OCCASION);
        offer.setQuantityInStock(quantityInStock);
        return offer;
    }

    /** A product of no vertical whose min price is the expired 1 EUR backmarket.fr offer. */
    private static Product indexedWithExpiredMinPrice(ElasticsearchOperations operations) {
        AggregatedPrice expired = offer("backmarket.fr", 1.0, EXPIRED, 5);
        AggregatedPrices price = new AggregatedPrices();
        price.setOffers(new HashSet<>(Set.of(expired)));
        price.setMinPrice(expired);
        price.setConditions(new HashSet<>(Set.of(ProductCondition.OCCASION)));
        price.setTrends(new HashMap<>(Map.of(ProductCondition.OCCASION, -1)));

        Product product = new Product(nextId++);
        product.setLastChange(EXPIRED);
        product.setOffersCount(1);
        product.setPrice(price);
        operations.save(product, ProductRepository.CURRENT_INDEX);
        return product;
    }

    private static ProductPartialUpdateHolder partial(Product product, AggregatedPrices price, int offersCount) {
        ProductPartialUpdateHolder partial = new ProductPartialUpdateHolder(product.getId());
        partial.addChange(ProductPartialUpdateField.LAST_CHANGE, NOW);
        partial.addChange(ProductPartialUpdateField.PRICE, price);
        partial.addChange(ProductPartialUpdateField.OFFERS_COUNT, offersCount);
        return partial;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> indexedPrice(ElasticsearchOperations operations, Product product) {
        operations.indexOps(ProductRepository.CURRENT_INDEX).refresh();
        Map<String, Object> source = operations.get(String.valueOf(product.getId()), Map.class,
                ProductRepository.CURRENT_INDEX);
        assertThat(source).containsEntry("lastChange", NOW);
        return (Map<String, Object>) source.get("price");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("mappers")
    void docMergeKeepsTheExpiredMinPrice(String name, JsonpMapper mapper) {
        // The former write: documents why the fix replaces fields instead of merging them
        ElasticsearchOperations operations = operations(mapper);
        Product product = indexedWithExpiredMinPrice(operations);
        Map<String, Object> changes = partial(product, new AggregatedPrices(), 0).getChanges();

        operations.bulkUpdate(List.of(UpdateQuery.builder(String.valueOf(product.getId()))
                .withDocument(Document.from(changes))
                .withIndex(ProductRepository.CURRENT_INDEX.getIndexName())
                .build()), ProductRepository.CURRENT_INDEX);

        Map<String, Object> price = indexedPrice(operations, product);
        assertThat(price.get("offers")).asList().isEmpty();
        assertThat(price).containsKey("minPrice");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("mappers")
    void partialUpdateRemovesTheExpiredMinPrice(String name, JsonpMapper mapper) {
        ElasticsearchOperations operations = operations(mapper);
        ProductRepository repository = new ProductRepository(operations);
        Product product = indexedWithExpiredMinPrice(operations);
        // What PriceService.onProduct gives once the offer is older than 2 days
        AggregatedPrices recomputed = new AggregatedPrices();
        recomputed.setLastOfferSeen(new HashMap<>(Map.of(ProductCondition.OCCASION, EXPIRED)));

        repository.bulkUpdateDocument(List.of(partial(product, recomputed, 0)));

        Map<String, Object> price = indexedPrice(operations, product);
        assertThat(price.get("minPrice")).isNull();
        assertThat(price.get("offers")).asList().isEmpty();
        assertThat(price.get("conditions")).asList().isEmpty();
        assertThat(price.get("trends")).asInstanceOf(MAP).isEmpty();
        assertThat(price.get("lastOfferSeen")).asInstanceOf(MAP)
                .containsEntry("OCCASION", EXPIRED);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("mappers")
    void partialUpdateReplacesTheMinPriceWithoutMixingOffers(String name, JsonpMapper mapper) {
        ElasticsearchOperations operations = operations(mapper);
        ProductRepository repository = new ProductRepository(operations);
        Product product = indexedWithExpiredMinPrice(operations);
        AggregatedPrice fresh = offer("other.fr", 120.0, NOW, null);
        AggregatedPrices recomputed = new AggregatedPrices();
        recomputed.setOffers(new HashSet<>(Set.of(fresh)));
        recomputed.setMinPrice(fresh);
        recomputed.setConditions(new HashSet<>(Set.of(ProductCondition.OCCASION)));

        repository.bulkUpdateDocument(List.of(partial(product, recomputed, 1)));

        Map<String, Object> price = indexedPrice(operations, product);
        assertThat(price.get("minPrice")).asInstanceOf(MAP)
                .containsEntry("datasourceName", "other.fr")
                .containsEntry("price", 120.0)
                .doesNotContainKey("quantityInStock");
        assertThat(price.get("offers")).asList().hasSize(1);
        assertThat(price.get("trends")).asInstanceOf(MAP).isEmpty();
    }
}
