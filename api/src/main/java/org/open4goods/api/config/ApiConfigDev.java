package org.open4goods.api.config;

import org.open4goods.api.config.yml.BackupConfig;
import org.open4goods.api.services.AggregationFacadeService;
import org.open4goods.api.services.backup.BackupService;
import org.open4goods.services.productrepository.services.ProductRepository;
import org.open4goods.services.serialisation.service.SerialisationService;
import org.open4goods.xwiki.services.XWikiReadService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile({ "dev", "devsec" })
public class ApiConfigDev {

	@Bean
	BackupService backupService(XWikiReadService xwikiService, ProductRepository productRepository, BackupConfig backupConfig, SerialisationService serialisationService, AggregationFacadeService aggregationService) {
		return new BackupService(xwikiService, productRepository, backupConfig, serialisationService, aggregationService);
	}

}
