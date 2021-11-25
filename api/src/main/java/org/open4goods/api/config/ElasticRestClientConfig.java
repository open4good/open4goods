package org.open4goods.api.config;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.open4goods.api.config.yml.ApiProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.common.collect.Iterables;

/**
 * The class for elastic-search configuration. 
 * @author goulven
 *
 */
@Configuration
public class ElasticRestClientConfig {

  @Autowired ApiProperties props;
	
  @Bean(destroyMethod = "close", name = "esHighLevelRestClient")
  public RestHighLevelClient highLevelClient() {
    return new RestHighLevelClient(restClientBuilder());
  }

  @Bean(destroyMethod = "close")
  public RestClient restClient() {
    return restClientBuilder().build();
  }

  private RestClientBuilder restClientBuilder() {
    // you can set N hosts
    List<HttpHost> hosts = new ArrayList<>();
    hosts.add(new HttpHost(
            props.getElasticSearchHost(),
            props.getElasticSearchPort(),
            "http"));

    return RestClient.builder(Iterables.toArray(hosts, HttpHost.class))
            .setRequestConfigCallback(requestConfigBuilder -> requestConfigBuilder
                    .setConnectTimeout(300000)
                    .setSocketTimeout(300000)
                    .setContentCompressionEnabled(false)
            		)
            		
            ;
  }}