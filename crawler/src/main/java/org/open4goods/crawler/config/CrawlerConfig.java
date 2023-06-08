package org.open4goods.crawler.config;

import org.apache.commons.lang3.ArrayUtils;
import org.open4goods.crawler.config.yml.FetcherProperties;
import org.open4goods.crawler.services.ApiSynchService;
import org.open4goods.crawler.services.DataFragmentCompletionService;
import org.open4goods.crawler.services.FetchersService;
import org.open4goods.crawler.services.IndexationService;
import org.open4goods.crawler.services.fetching.CsvDatasourceFetchingService;
import org.open4goods.crawler.services.fetching.WebDatasourceFetchingService;
import org.open4goods.services.EvaluationService;
import org.open4goods.services.SerialisationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class CrawlerConfig {

	

	protected @Autowired Environment env;
	
	
    @Bean
    DataFragmentCompletionService offerCompletionService(@Autowired final FetcherProperties fetcherProperties) {
		return new DataFragmentCompletionService();
	}

    @Bean
    SerialisationService serialisationService() {
		return new SerialisationService();
	}

    /**
     * The service that hot evaluates thymeleaf / spel expressions
     * @return
     */
     @Bean EvaluationService evaluationService() {
		return new EvaluationService();
	}

    @Bean
    WebDatasourceFetchingService webDatasourceFetchingService(@Autowired final FetcherProperties fetcherProperties,
                                                                      @Autowired final IndexationService indexationService) {
    	
		// Logging to console according to dev profile and conf
		boolean toConsole = false;
		// TODO : Not nice, mutualize
		if (ArrayUtils.contains(env.getActiveProfiles(), "dev") || ArrayUtils.contains(env.getActiveProfiles(), "devsec")) {
			toConsole = true;
		}
		
		
		return new WebDatasourceFetchingService(indexationService, fetcherProperties, fetcherProperties.getLogsDir(), toConsole);
	}

    @Bean
    CsvDatasourceFetchingService csvDatasourceFetchingService(
            @Autowired final DataFragmentCompletionService completionService,
            @Autowired final FetcherProperties fetcherProperties,
            @Autowired final WebDatasourceFetchingService webDatasourceFetchingService,
            @Autowired final IndexationService indexationService
            ) {
    	
		// Logging to console according to dev profile and conf
		boolean toConsole = false;
		// TODO : Not nice, mutualize
		if (ArrayUtils.contains(env.getActiveProfiles(), "dev") || ArrayUtils.contains(env.getActiveProfiles(), "devsec")) {
			toConsole = true;
		}
		
		
		return new CsvDatasourceFetchingService(completionService, indexationService, fetcherProperties, webDatasourceFetchingService,fetcherProperties.getLogsDir(),  toConsole);
	}

    @Bean
    IndexationService indexationService(@Autowired final FetcherProperties fetcherProperties) {
		return new IndexationService(fetcherProperties.indexationEndpoint(), fetcherProperties.getApiKey());
	}

    @Bean
    ApiSynchService apiSynchService(@Autowired final FetcherProperties fetcherProperties,
                                            @Autowired final FetchersService fetchersService) {
		return new ApiSynchService(fetcherProperties.getApiSynchConfig(), fetchersService,
				fetcherProperties.getMasterEndpoint(), fetcherProperties.getApiKey());
	}


    @Bean
    FetchersService fetchersService(@Autowired final FetcherProperties fetcherProperties,
                                             @Autowired final WebDatasourceFetchingService webDatasourceFetchingService,
                                             @Autowired final CsvDatasourceFetchingService csvDatasourceFetchingService
                                             ) {
		return new FetchersService(fetcherProperties, webDatasourceFetchingService, csvDatasourceFetchingService);
	}

	
}
