package org.open4goods.nudgerfrontapi.config;

import org.open4goods.commons.services.TextEmbeddingService;
import org.open4goods.nudgerfrontapi.config.properties.EmbeddingClientProperties;
import org.open4goods.nudgerfrontapi.service.http.EmbeddingGatewayClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Configuration wiring the HTTP client that proxies embedding calls to the gateway.
 */
@Configuration
@EnableConfigurationProperties(EmbeddingClientProperties.class)
public class EmbeddingClientConfig
{
    @Bean
    ClientHttpRequestFactory embeddingClientHttpRequestFactory(EmbeddingClientProperties properties)
    {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) properties.getConnectTimeout().toMillis());
        factory.setReadTimeout((int) properties.getReadTimeout().toMillis());
        return factory;
    }

    @Bean
    RestClient embeddingRestClient(EmbeddingClientProperties properties, ClientHttpRequestFactory embeddingClientHttpRequestFactory)
    {
        return RestClient.builder()
                .requestFactory(embeddingClientHttpRequestFactory)
                .baseUrl(properties.getBaseUrl())
                .build();
    }

    @Bean
    @ConditionalOnMissingBean(TextEmbeddingService.class)
    TextEmbeddingService embeddingGatewayClient(RestClient embeddingRestClient)
    {
        return new EmbeddingGatewayClient(embeddingRestClient);
    }
}
