package org.open4goods.ui.config;

import org.open4goods.ui.config.yml.UiConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

@Configuration
/**
 * The chain for thymeleaf templates resolving. They are resolved in the
 * following order :
 * <ul>
 * <li>FileSystem template location</li>
 * <li>FileSystem default location</li>
 * <li>classpath template location</li>
 * <li>classpath default location</li>
 * </ul>
 * 
 * @author goulven
 *
 */
public class TemplatesConfig {

	private @Autowired UiConfig config;

	@Bean
	public ClassLoaderTemplateResolver forthTemplateResolver() {
		ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
		templateResolver.setPrefix("templates/");
		templateResolver.setSuffix(".html");
		templateResolver.setTemplateMode(TemplateMode.HTML);
		templateResolver.setCharacterEncoding("UTF-8");
		templateResolver.setOrder(3);
		templateResolver.setCheckExistence(true);
		templateResolver.setCacheable(config.getWebConfig().getTemplatesCaching());
		return templateResolver;
	}
	
}