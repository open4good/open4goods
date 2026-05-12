
package org.open4goods.crawler;

import org.open4goods.crawler.repository.IndexationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Main entry point for the open4goods crawler application. */
@SpringBootApplication
@EnableScheduling
@EnableElasticsearchRepositories(basePackageClasses = IndexationRepository.class)
public class Crawler {

    protected static final Logger logger = LoggerFactory.getLogger(Crawler.class);

    public static void main(final String[] args) {
        SpringApplication.run(Crawler.class, args);
    }

}
