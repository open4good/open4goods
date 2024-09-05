package org.open4goods.crawler.config;

import org.apache.commons.lang3.ArrayUtils;
import org.open4goods.commons.services.EvaluationService;
import org.open4goods.commons.services.RemoteFileCachingService;
import org.open4goods.commons.services.SerialisationService;
import org.open4goods.crawler.config.yml.FetcherProperties;
import org.open4goods.crawler.repository.IndexationRepository;
import org.open4goods.crawler.repository.IndexationRepository;
import org.open4goods.crawler.services.ApiSynchService;
import org.open4goods.crawler.services.DataFragmentCompletionService;
import org.open4goods.crawler.services.FetchersService;
import org.open4goods.crawler.services.IndexationService;
import org.open4goods.crawler.services.fetching.CsvDatasourceFetchingService;
import org.open4goods.crawler.services.fetching.WebDatasourceFetchingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class CrawlerConfig {

	

	protected final Environment env;

    public CrawlerConfig(Environment env) {
        this.env = env;
    }




    
//    @Bean
//    AwinCatalogService awinCatalogService(@Autowired final FetcherProperties fetcherProperties) {
//		return new AwinCatalogService(fetcherProperties.getAwinCatalogUrl());
//	}
    
    
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
                                                                      @Autowired final IndexationService indexationService,  @Autowired final IndexationRepository indexationRepository) {
    	
        return new WebDatasourceFetchingService(indexationService, fetcherProperties, indexationRepository, fetcherProperties.getLogsDir());
	}

 
    @Bean
    CsvDatasourceFetchingService csvDatasourceFetchingService(
            @Autowired final DataFragmentCompletionService completionService,
            @Autowired final FetcherProperties fetcherProperties,
            @Autowired final WebDatasourceFetchingService webDatasourceFetchingService,
            @Autowired final IndexationService indexationService,
            @Autowired IndexationRepository indexationRepository,
//            @Autowired AwinCatalogService awinCatalogService,
            @Autowired IndexationRepository csvIndexationRepo,
            @Autowired RemoteFileCachingService remoteFileCachingService

            ) {
    	

        return new CsvDatasourceFetchingService(csvIndexationRepo, completionService, indexationService, fetcherProperties, webDatasourceFetchingService, indexationRepository,webDatasourceFetchingService, remoteFileCachingService, fetcherProperties.getLogsDir());
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
