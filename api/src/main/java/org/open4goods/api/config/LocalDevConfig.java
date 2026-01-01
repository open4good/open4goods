package org.open4goods.api.config;

import java.lang.reflect.Proxy;

import org.open4goods.api.repository.mock.MockProductRepository;
import org.open4goods.api.services.backup.BackupService;
import org.open4goods.api.config.yml.BackupConfig;
import org.open4goods.services.productrepository.services.ProductRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.open4goods.embedding.config.DjlEmbeddingProperties;
import org.open4goods.embedding.service.image.AbstractImageModelFactory;
import org.open4goods.embedding.service.image.DjlImageEmbeddingService;
import org.open4goods.brand.model.BrandScore;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter;
import org.springframework.data.elasticsearch.core.mapping.SimpleElasticsearchMappingContext;

@Configuration
@Profile("local")
public class LocalDevConfig {

    /**
     * Provides a dummy ElasticsearchOperations bean to satisfy dependency injection
     * and alias it to elasticsearchTemplate.
     */
    @Bean(name = { "elasticsearchOperations", "elasticsearchTemplate" })
    @Primary
    ElasticsearchOperations elasticsearchTemplate() {
        return (ElasticsearchOperations) Proxy.newProxyInstance(
            ElasticsearchOperations.class.getClassLoader(),
            new Class[] { ElasticsearchOperations.class },
            (proxy, method, args) -> {
                // System.out.println("MockElasticsearchOperations called: " + method.getName());
                if (method.getName().equals("getElasticsearchConverter")) {
                    return Proxy.newProxyInstance(
                        ElasticsearchConverter.class.getClassLoader(),
                        new Class[] { ElasticsearchConverter.class },
                        (p, m, a) -> {
                            if (m.getName().equals("getMappingContext")) {
                                SimpleElasticsearchMappingContext context = new SimpleElasticsearchMappingContext();
                                context.setInitialEntitySet(Collections.singleton(BrandScore.class));
                                context.afterPropertiesSet();
                                return context;
                            }
                            return null;
                        });
                }
                if (method.getName().equals("indexOps")) {
                     return Proxy.newProxyInstance(
                        IndexOperations.class.getClassLoader(),
                        new Class[] { IndexOperations.class },
                        (p2, m2, a2) -> {
                            if (m2.getName().equals("exists")) return true;
                            if (m2.getReturnType().equals(boolean.class)) return true;
                            return null;
                        });
                }
                
                if (method.getName().equals("toString")) return "MockElasticsearchOperations";
                if (method.getName().equals("hashCode")) return 42;
                if (method.getName().equals("equals")) return false;

                if (method.getReturnType().equals(long.class)) {
                    return 0L;
                }
                if (method.getReturnType().equals(List.class)) {
                    return new ArrayList<>();
                }
                if (method.getReturnType().equals(boolean.class)) {
                    return false;
                }
                if (method.getReturnType().equals(String.class)) {
                    return "mock";
                }
                // Return defaults for primitives to avoid NPE on unboxing if called
                if (method.getReturnType().equals(int.class)) return 0;
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
	@Primary
	BackupService backupService() {
		return new BackupService(null, null, new BackupConfig(), null, null);
	}
	
	@Bean
	@Primary
	DjlImageEmbeddingService imageEmbeddingService() {
		DjlEmbeddingProperties properties = new DjlEmbeddingProperties();
		AbstractImageModelFactory factory = new AbstractImageModelFactory()
		{
			@Override
			public ai.djl.repository.zoo.ZooModel<ai.djl.modality.cv.Image, float[]> loadModel(String modelUrl, int imageSize) {
				return null;
			}
		};

		return new DjlImageEmbeddingService(properties, factory)
		{
			@Override
			public void initialize()
			{
				// no-op for local profile
			}

			@Override
			public float[] embed(Path imagePath)
			{
				return new float[0];
			}
		};
	}
}
