package org.open4goods.ui.config;

import java.io.File;
import java.util.List;

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
@Profile({"dev","docker"})
public class AppConfigDev {
	
	private @Autowired UiConfig config;
	
	
	@Bean
	public MessageSource messageSource()
	{       
	    ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
	    messageSource.setBasenames( "file:" + config.resourceBundleFolder(), "classpath:/i18n/messages" );
	    messageSource.setCacheMillis(0);
//	    messageSource.setDefaultEncoding( );
	    return messageSource;
	}
	
	
	///////////////////////////////////
	// Resources
	///////////////////////////////////
	@Bean
	WebMvcConfigurer configurer() {
		return new WebMvcConfigurer() {

			@Override
			public void extendMessageConverters(final List<HttpMessageConverter<?>> converters) {
				converters.add(new YamlHttpMessageConverter<>());
			}

			@Override
			public void addInterceptors(final InterceptorRegistry registry) {
				registry.addInterceptor(AppConfig.localeChangeInterceptor());
				registry.addInterceptor(new GenericTemplateInterceptor());
			}

			@Override
			public void configurePathMatch(PathMatchConfigurer configurer) {
				UrlPathHelper urlPathHelper = new UrlPathHelper();
				urlPathHelper.setUrlDecode(false);
				configurer.setUrlPathHelper(urlPathHelper);
			}


			/**
			 * Define explicitly each static resources (because of the controller /* in the
			 * CommonPageCOntroller) Also overrides classpath values with the ones pageNumber
			 * filesystem
			 */
			@Override
			public void addResourceHandlers(final ResourceHandlerRegistry registry) {

				registry.setOrder(Ordered.LOWEST_PRECEDENCE);
				registry.addResourceHandler("/sitemap/**").addResourceLocations("file:" + config.siteMapFolder().getAbsolutePath() + File.separator);

				// NOTE : Register here files / folder to be allowed in devmode
				registerFolder(registry,"assets");
				registerFolder(registry,"css");
				registerFolder(registry,"icons");
				registerFolder(registry,"vendor");
				registerFile(registry,"tpl_table.html");
			}
		};
	}

	/**
	 * Register a chained resolver (file first, then classpath) allowing local
	 * templates edition, for a folder
	 * 
	 * @param registry
	 * @param location
	 */
	public void registerFolder(ResourceHandlerRegistry registry, String location) {
		registry.addResourceHandler("/" + location + "/**")
				.addResourceLocations("file:" + config.getResourceTemplateFolder() + "static" + File.separator + location+ File.separator, "classpath:/static/" + location + "/")
				.setCacheControl(CacheControl.noCache())
				;
	}

	
	/**
	 * Register a chained resolver (file first, then classpath) allowing local
	 * templates edition, for a folder
	 * 
	 * @param registry
	 * @param location
	 */
	public void registerFile(ResourceHandlerRegistry registry, String location) {
		registry.addResourceHandler("/" + location )
				.addResourceLocations("file:" + config.getResourceTemplateFolder() + "static" + File.separator + location, "classpath:/static/" + location )
				.setCacheControl(CacheControl.noCache())
				;
	}	

}
