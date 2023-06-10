package org.open4goods.ui.config;

import org.open4goods.ui.config.yml.UiConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.ViewResolver;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.templateresolver.FileTemplateResolver;

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
	public ClassLoaderTemplateResolver classpathTemplateResolver() {
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


	@Bean
	public FileTemplateResolver fileTemplateResolver() {
		final FileTemplateResolver resolver = new FileTemplateResolver();

		////////////////////////////////////////
		// First, get from the "specific" theme
		////////////////////////////////////////

		resolver.setPrefix(config.getThymeLeafTemplateFolder());
		resolver.setSuffix(".html");
		resolver.setTemplateMode(TemplateMode.HTML);
		resolver.setOrder(0);
		// TODO(perf,p3,0.1) : test disabling this feature
		resolver.setCacheable(config.getWebConfig().getTemplatesCaching());
		resolver.setCheckExistence(true);
		return resolver;

	}
	
    @Bean
    public SpringTemplateEngine templateEngine() {
        /* SpringTemplateEngine automatically applies SpringStandardDialect and
           enables Spring's own MessageSource message resolution mechanisms. */
        SpringTemplateEngine templateEngine = new SpringTemplateEngine();

        // TODO : Disable file template resolver in production
        templateEngine.addTemplateResolver(this.classpathTemplateResolver());
        
        
        templateEngine.addTemplateResolver(this.fileTemplateResolver());

        templateEngine.setEnableSpringELCompiler(true);
        return templateEngine;
    }

    @Bean
    public ViewResolver viewResolver() {
        ThymeleafViewResolver viewResolver = new ThymeleafViewResolver();
        viewResolver.setTemplateEngine(this.templateEngine());
        viewResolver.setCharacterEncoding("UTF-8");
        // TODO : from conf
        viewResolver.setCache(false);   /* FYI: during development -> false */
        viewResolver.setOrder(1);
        return viewResolver;
    }

}