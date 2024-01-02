package org.open4goods.ui.config;

import java.util.List;

import org.open4goods.ui.interceptors.GenericTemplateInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.util.UrlPathHelper;

@Configuration
@Profile({"beta","prod"})
public class AppConfigProd {


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
		};
	}
	

}
