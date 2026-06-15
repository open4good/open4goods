
package org.open4goods.ui;

import org.open4goods.model.CacheKeyGenerator;
import org.open4goods.services.productrepository.repository.ElasticProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Main entry point for the open4goods UI (legacy) application. */
@SpringBootApplication(scanBasePackages = {"org.open4goods.services"}, scanBasePackageClasses = { Ui.class, CacheKeyGenerator.class})
@EnableScheduling
@EnableCaching
@Configuration
@ConfigurationPropertiesScan
@EnableElasticsearchRepositories(basePackageClasses = {ElasticProductRepository.class})
public class Ui {

    private static final Logger logger = LoggerFactory.getLogger(Ui.class);

    public static void main(final String[] args) {
        System.setProperty("org.apache.tomcat.util.buf.UDecoder.ALLOW_ENCODED_SLASH", "true");
        SpringApplication.run(Ui.class, args);
    }

}
