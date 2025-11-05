package org.open4goods.eprelservice;

import org.open4goods.eprelservice.config.EprelServiceProperties;
import org.open4goods.eprelservice.repository.EprelProductRepository;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Spring Boot entry point for the EPREL microservice.
 */
@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(EprelServiceProperties.class)
@EnableElasticsearchRepositories(basePackageClasses = EprelProductRepository.class)
public class EprelServiceApplication
{
    /**
     * Bootstraps the EPREL microservice.
     *
     * @param args runtime arguments
     */
    public static void main(String[] args)
    {
        SpringApplication.run(EprelServiceApplication.class, args);
    }
}
