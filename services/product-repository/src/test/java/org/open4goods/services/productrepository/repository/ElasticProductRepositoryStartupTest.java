package org.open4goods.services.productrepository.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.open4goods.model.product.Product;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchCustomConversions;
import org.springframework.data.elasticsearch.core.convert.MappingElasticsearchConverter;
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentEntity;
import org.springframework.data.elasticsearch.core.mapping.SimpleElasticsearchMappingContext;
import org.springframework.data.elasticsearch.repository.support.ElasticsearchRepositoryFactory;

/**
 * Building {@link ElasticProductRepository} never calls Elasticsearch (GOU-1835): this proxy is the only products
 * index access ui creates at startup, and a failing {@code exists}/{@code PUT _mapping} in its constructor used to
 * fail the whole ui (and api) startup (GOU-1812). The api applies the mapping later, in
 * {@code ProductIndexMappingRunner}.
 */
class ElasticProductRepositoryStartupTest {

    @Test
    void productEntity_neitherCreatesItsIndexNorWritesItsMapping() {
        ElasticsearchPersistentEntity<?> entity = mappingContext().getRequiredPersistentEntity(Product.class);

        assertThat(entity.isCreateIndexAndMapping()).isFalse();
        assertThat(entity.isAlwaysWriteMapping()).isFalse();
        // Still read by createIndex(), createWithMapping() and putMapping().
        assertThat(entity.settingPath()).isEqualTo("/product-settings.json");
        assertThat(Product.class.getResource("/product-mappings.json")).isNotNull();
    }

    @Test
    void repositoryIsBuiltWhenEveryIndexOperationFails() {
        IndexOperations failingIndexOperations = mock(IndexOperations.class, invocation -> {
            throw new DataAccessResourceFailureException("Elasticsearch unreachable: " + invocation.getMethod());
        });
        ElasticsearchOperations operations = mock(ElasticsearchOperations.class);
        when(operations.getElasticsearchConverter())
                .thenReturn(new MappingElasticsearchConverter(mappingContext()));
        when(operations.indexOps(any(Class.class))).thenReturn(failingIndexOperations);

        ElasticProductRepository repository = new ElasticsearchRepositoryFactory(operations)
                .getRepository(ElasticProductRepository.class);

        assertThat(repository).isNotNull();
        verifyNoInteractions(failingIndexOperations);
    }

    /** The mapping context as Spring Boot builds it: BigDecimal and the other store types are simple types. */
    private static SimpleElasticsearchMappingContext mappingContext() {
        SimpleElasticsearchMappingContext mappingContext = new SimpleElasticsearchMappingContext();
        mappingContext.setSimpleTypeHolder(new ElasticsearchCustomConversions(List.of()).getSimpleTypeHolder());
        return mappingContext;
    }
}
