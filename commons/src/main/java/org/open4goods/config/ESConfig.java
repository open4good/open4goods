package org.open4goods.config;

import java.net.UnknownHostException;
import java.time.Duration;

import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.ClientConfiguration.TerminalClientConfigurationBuilder;
import org.springframework.data.elasticsearch.client.RestClients;
import org.springframework.data.elasticsearch.config.AbstractElasticsearchConfiguration;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;

@Configuration
public class ESConfig extends AbstractElasticsearchConfiguration {

    @Value("${elasticsearch.host:localhost}")
    private String host;

    @Value("${elasticsearch.port:9200}")
    private int port;

    @Value("${elasticsearch.username}")
    private String username;

    @Value("${elasticsearch.password}")
    private String password;

    
    @Bean
    @Override
    public RestHighLevelClient elasticsearchClient() {
        TerminalClientConfigurationBuilder builder = ClientConfiguration.builder()
                .connectedTo(host+ ":" + port)
//                .usingSsl() 
                .withBasicAuth(username, password)
                //TODO : timeout from config
                .withConnectTimeout(Duration.ofSeconds(30))
                .withSocketTimeout(Duration.ofSeconds(30))
                ; 
        final ClientConfiguration clientConfiguration = builder.build();
        return RestClients.create(clientConfiguration).rest();
    }
    
    @Bean
    public ElasticsearchRestTemplate elasticsearchRestTemplate() throws UnknownHostException {
        return new ElasticsearchRestTemplate(elasticsearchClient());
    }
}