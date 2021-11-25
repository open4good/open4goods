package org.open4goods.crawler.config;

import org.open4goods.crawler.config.yml.FetcherProperties;
import org.open4goods.crawler.services.ApiSynchService;
import org.open4goods.crawler.services.DataFragmentCompletionService;
import org.open4goods.crawler.services.FetchersService;
import org.open4goods.crawler.services.IndexationService;
import org.open4goods.crawler.services.fetching.AggregatedDataBackedFetchingService;
import org.open4goods.crawler.services.fetching.CsvDatasourceFetchingService;
import org.open4goods.crawler.services.fetching.WebDatasourceFetchingService;
import org.open4goods.services.EvaluationService;
import org.open4goods.services.SerialisationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;

@Configuration
public class CrawlerConfig {

	@Bean
	public DataFragmentCompletionService offerCompletionService(@Autowired final FetcherProperties fetcherProperties) {
		return new DataFragmentCompletionService();
	}

	@Bean
	public SerialisationService serialisationService() {
		return new SerialisationService();
	}

	/**
	 * The service that hot evaluates thymeleaf / spel expressions
	 * @return
	 */
	public @Bean EvaluationService evaluationService() {
		return new EvaluationService();
	}

	@Bean
	public WebDatasourceFetchingService webDatasourceFetchingService(@Autowired final FetcherProperties fetcherProperties,
			@Autowired final IndexationService indexationService) {
		return new WebDatasourceFetchingService(indexationService, fetcherProperties, fetcherProperties.getLogsDir());
	}

	@Bean
	public CsvDatasourceFetchingService csvDatasourceFetchingService(
			@Autowired final DataFragmentCompletionService completionService,
			@Autowired final FetcherProperties fetcherProperties,
			@Autowired final WebDatasourceFetchingService webDatasourceFetchingService,
			@Autowired final IndexationService indexationService			
			) {
		return new CsvDatasourceFetchingService(completionService, indexationService, fetcherProperties, webDatasourceFetchingService,fetcherProperties.getLogsDir());
	}

	@Bean
	public IndexationService indexationService(@Autowired final FetcherProperties fetcherProperties) {
		return new IndexationService(fetcherProperties.indexationEndpoint(), fetcherProperties.getApiKey());
	}

	@Bean
	public ApiSynchService apiSynchService(@Autowired final FetcherProperties fetcherProperties,
			@Autowired final FetchersService fetchersService) {
		return new ApiSynchService(fetcherProperties.getApiSynchConfig(), fetchersService,
				fetcherProperties.getMasterEndpoint(), fetcherProperties.getApiKey());
	}


	@Bean
	public FetchersService fetchersService(@Autowired final FetcherProperties fetcherProperties,
			@Autowired final WebDatasourceFetchingService webDatasourceFetchingService,
			@Autowired final CsvDatasourceFetchingService csvDatasourceFetchingService,
			@Autowired final AggregatedDataBackedFetchingService apiDatasourceFetchingService
			) {
		return new FetchersService(fetcherProperties, webDatasourceFetchingService, csvDatasourceFetchingService,apiDatasourceFetchingService);
	}

	/**
	 * Swagger configuration
	 * @return
	 */
	@Bean
	public Docket docket() {
		//TODO(conf) : from conf
		final ApiInfo apiInfo = new ApiInfoBuilder().title("Fetcher").description("The fetcher API").version("0.1").build();

		return new Docket(DocumentationType.SWAGGER_2).apiInfo(apiInfo).select()
				.apis(RequestHandlerSelectors.basePackage("org.open4goods")).paths(PathSelectors.any()).build();
	}
}
