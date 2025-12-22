package org.open4goods.nudgerfrontapi.config;

import java.lang.reflect.Proxy;

import org.open4goods.nudgerfrontapi.repository.mock.MockProductRepository;
import org.open4goods.services.productrepository.services.ProductRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Primary;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;

@Configuration
@Profile("local")
public class LocalDevConfig {

    /**
     * Provides a dummy ElasticsearchOperations bean to satisfy dependency injection
     * for the ProductRepository superclass, avoiding the need for a real Elastic connection.
     */
    @Bean(name = { "elasticsearchOperations", "elasticsearchTemplate" })
    public ElasticsearchOperations elasticsearchOperations() {
        return (ElasticsearchOperations) Proxy.newProxyInstance(
            ElasticsearchOperations.class.getClassLoader(),
            new Class[] { ElasticsearchOperations.class },
            (proxy, method, args) -> {
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
