package org.open4goods.ui.config;

import org.open4goods.ui.config.yml.UiConfig;
import org.open4goods.ui.services.BlogService;
import org.open4goods.xwiki.services.XwikiFacadeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

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
	
	
    @Bean
    BlogService blogService(@Autowired XwikiFacadeService xwikiReadService, @Autowired UiConfig config) {
		return new BlogService(xwikiReadService, null, null) {
			@Override
			public void updateBlogPosts() {
			}
		};
	}
		

}
