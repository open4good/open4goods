package org.open4goods.nudgerfrontapi.config;

import java.lang.reflect.Proxy;
import java.util.Collections;

import org.open4goods.brand.service.BrandService;
import org.open4goods.nudgerfrontapi.repository.mock.MockProductRepository;
import org.open4goods.services.productrepository.services.ProductRepository;
import org.open4goods.services.remotefilecaching.service.RemoteFileCachingService;
import org.open4goods.services.serialisation.service.SerialisationService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Primary;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter;
import org.springframework.data.elasticsearch.core.mapping.SimpleElasticsearchMappingContext;

@Configuration
@Profile("local")
public class LocalDevConfig {

    /**
     * Provides a dummy ElasticsearchOperations bean to satisfy dependency injection
     * for the ProductRepository superclass, and to let {@code @EnableElasticsearchRepositories}
     * create its repository proxies without a real Elastic connection: Spring Data Elasticsearch
     * resolves {@code getElasticsearchConverter().getMappingContext()} while building those proxies,
     * so this stub must answer it with a real, usable {@link SimpleElasticsearchMappingContext}
     * rather than {@code null}.
     */
    @Bean(name = { "elasticsearchOperations", "elasticsearchTemplate" })
    public ElasticsearchOperations elasticsearchOperations() {
        SimpleElasticsearchMappingContext mappingContext = new SimpleElasticsearchMappingContext();
        mappingContext.setInitialEntitySet(Collections.emptySet());
        mappingContext.afterPropertiesSet();

        return (ElasticsearchOperations) Proxy.newProxyInstance(
            ElasticsearchOperations.class.getClassLoader(),
            new Class[] { ElasticsearchOperations.class },
            (proxy, method, args) -> {
                if (method.getName().equals("getElasticsearchConverter")) {
                    return Proxy.newProxyInstance(
                        ElasticsearchConverter.class.getClassLoader(),
                        new Class[] { ElasticsearchConverter.class },
                        (converterProxy, converterMethod, converterArgs) -> {
                            if (converterMethod.getName().equals("getMappingContext")) {
                                return mappingContext;
                            }
                            return null;
                        });
                }
                // Return defaults for primitives to avoid NPE on unboxing if called
                if (method.getReturnType().equals(boolean.class)) return false;
                if (method.getReturnType().equals(int.class)) return 0;
                if (method.getReturnType().equals(long.class)) return 0L;
                if (method.getReturnType().equals(double.class)) return 0.0;
                return null;
            }
        );
    }

    /**
     * Mocks the ProductRepository to return empty/dummy data instead of querying ElasticSearch.
     */
    @Bean
    @Primary
    public ProductRepository productRepository() {
        return new MockProductRepository();
    }

    /**
     * Builds the {@link BrandService} from an empty in-memory referential instead of
     * {@code AppConfig}'s GitHub-backed one: {@code BrandService}'s constructor loads the
     * referential synchronously and throws if that load fails, and
     * {@code RemoteFileCachingService.getResource()} has no cached copy to fall back to on a
     * first, offline run.
     */
    @Bean
    public BrandService brandService(RemoteFileCachingService remoteFileCachingService,
            SerialisationService serialisationService) throws Exception {
        return new BrandService(remoteFileCachingService, serialisationService,
                () -> "{\"version\":3,\"brands\":[]}",
                companyId -> {
                    throw new UnsupportedOperationException("No company loader in the local profile");
                });
    }
    
    @Bean
    public org.open4goods.services.contribution.repository.ContributionVoteRepository contributionVoteRepository() {
    	return (org.open4goods.services.contribution.repository.ContributionVoteRepository) Proxy.newProxyInstance(
    			org.open4goods.services.contribution.repository.ContributionVoteRepository.class.getClassLoader(),
            new Class[] { org.open4goods.services.contribution.repository.ContributionVoteRepository.class },
            (proxy, method, args) -> {
             if (method.getReturnType().equals(java.util.Optional.class)) return java.util.Optional.empty();
             if (method.getReturnType().equals(Iterable.class)) return java.util.Collections.emptyList();
             if (method.getReturnType().equals(boolean.class)) return false;
             if (method.getReturnType().equals(long.class)) return 0L;
             return null;
            }
        );
    }
}
