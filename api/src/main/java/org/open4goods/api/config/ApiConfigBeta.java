package org.open4goods.api.config;

import org.open4goods.api.config.yml.ApiProperties;
import org.open4goods.api.services.BackupService;
import org.open4goods.dao.ProductRepository;
import org.open4goods.services.SerialisationService;
import org.open4goods.xwiki.services.XWikiReadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile({ "beta" })
public class ApiConfigBeta {

	private ApiProperties apiProperties;

	public ApiConfigBeta(ApiProperties apiProperties) {
		this.apiProperties = apiProperties;
	}
}
