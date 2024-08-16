package org.open4goods.ui.config;

import java.io.File;
import java.util.List;

import org.open4goods.dao.ProductRepository;
import org.open4goods.helper.DevModeService;
import org.open4goods.interceptors.BanCheckerInterceptor;
import org.open4goods.services.SerialisationService;
import org.open4goods.services.VerticalsConfigService;
import org.open4goods.ui.config.yml.UiConfig;
import org.open4goods.ui.interceptors.GenericTemplateInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.core.Ordered;
import org.springframework.http.CacheControl;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.util.UrlPathHelper;

@Configuration
@Profile({"dev","devsec"})
public class AppConfigDev {
	
	private final UiConfig config;

	public AppConfigDev(UiConfig config) {
		this.config = config;
	}



	
	@Bean
	public MessageSource messageSource()
	{       
	    ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
	    messageSource.setBasenames( "file:" + config.resourceBundleFolder(), "classpath:/i18n/messages" );
	    messageSource.setCacheMillis(0);
//	    messageSource.setDefaultEncoding( );
	    return messageSource;
	}
	
	

}
