package org.open4goods.ui.config;

import java.io.File;
import java.util.List;

import org.open4goods.commons.interceptors.BanCheckerInterceptor;
import org.open4goods.ui.config.yml.UiConfig;
import org.open4goods.ui.interceptors.GenericTemplateInterceptor;
import org.open4goods.ui.services.BlogService;
import org.open4goods.xwiki.services.XwikiFacadeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.util.UrlPathHelper;

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
