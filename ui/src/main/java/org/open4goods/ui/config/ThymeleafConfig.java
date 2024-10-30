package org.open4goods.ui.config;

import org.apache.commons.lang3.StringUtils;
import org.open4goods.ui.config.yml.UiConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.ViewResolver;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;

@Configuration
/**
 * Use the theme files and fallback to default templates if no specialized file exists
 */
public class ThymeleafConfig {

	private static final String CLASSPATH_TEMPLATES_PREFIX = "classpath:/templates";
	@Autowired UiConfig uiConfig;
	
    @Bean
    @ConditionalOnProperty(matchIfMissing = false, name = "webConfig.theme", havingValue = "")
    public SpringResourceTemplateResolver themeTemplateResolver() {
        SpringResourceTemplateResolver customResolver = new SpringResourceTemplateResolver();
        
        if (StringUtils.isEmpty(uiConfig.getWebConfig().getTemplatesPath())) {
        	customResolver.setPrefix(CLASSPATH_TEMPLATES_PREFIX+"/themes/"+uiConfig.getWebConfig().getTheme()+ "/");
        } else {
        	customResolver.setPrefix("file:"+uiConfig.getWebConfig().getTemplatesPath()+"/themes/"+uiConfig.getWebConfig().getTheme()+ "/");
        }
        
        customResolver.setSuffix(".html");
        customResolver.setOrder(1); // Higher priority
        customResolver.setCheckExistence(true);
        customResolver.setCacheable(uiConfig.getWebConfig().getTemplatesCaching());
        return customResolver;
    }

    @Bean
    SpringResourceTemplateResolver defaultTemplateResolver() {
        SpringResourceTemplateResolver defaultResolver = new SpringResourceTemplateResolver();
        if (StringUtils.isEmpty(uiConfig.getWebConfig().getTemplatesPath())) {
        	defaultResolver.setPrefix(CLASSPATH_TEMPLATES_PREFIX+"/default/");
        }
    	else {
        	defaultResolver.setPrefix("file:"+uiConfig.getWebConfig().getTemplatesPath()+"/default/");
    	}
        	
        defaultResolver.setSuffix(".html");
        defaultResolver.setOrder(2); // Lower priority
        defaultResolver.setCheckExistence(true);
        defaultResolver.setCacheable(uiConfig.getWebConfig().getTemplatesCaching());
        return defaultResolver;
    }
    
}