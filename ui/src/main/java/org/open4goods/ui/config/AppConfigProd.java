package org.open4goods.ui.config;

import org.open4goods.services.blog.service.BlogService;
import org.open4goods.ui.config.yml.UiConfig;
import org.open4goods.xwiki.services.XwikiFacadeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile({"beta","prod"})
public class AppConfigProd {


	private final UiConfig config;

	public AppConfigProd(UiConfig config) {
		this.config = config;
	}

    @Bean
    BlogService blogService(@Autowired XwikiFacadeService xwikiReadService, @Autowired UiConfig config) {
		return new BlogService(xwikiReadService, config.getBlogConfig(), config.getNamings().getBaseUrls());
	}

}
