
package org.open4goods.crawler.controller;

import java.io.IOException;

import org.open4goods.commons.config.yml.datasource.DataSourceProperties;
import org.open4goods.commons.model.crawlers.FetcherGlobalStats;
import org.open4goods.commons.model.dto.FetchRequestResponse;
import org.open4goods.crawler.config.yml.FetcherProperties;
import org.open4goods.crawler.services.FetchersService;
import org.open4goods.model.constants.UrlConstants;
import org.open4goods.model.datafragment.DataFragment;
import org.open4goods.model.exceptions.TechnicalException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
//TODO(gof) : Add SpringWebSecurity

public class CrawlController {

	private final FetchersService fetchersService;

	private final FetcherProperties config;

	public CrawlController(final FetchersService fetchersService, final FetcherProperties config) {
		super();
		this.fetchersService = fetchersService;
		this.config = config;
	}

	private static final Logger logger = LoggerFactory.getLogger(CrawlController.class);

	@PostMapping(path=UrlConstants.CRAWLER_API_REQUEST_FETCHING)
	public FetchRequestResponse fetch(@RequestBody final DataSourceProperties p, @RequestParam final String datasourceConfName) throws TechnicalException {

		final FetchRequestResponse ret = new FetchRequestResponse(true);

		
		if (null != p.getCsvDatasource() ) {
			logger.warn("{} will be ignored, CSV Jobs must be triggered through feedservice",datasourceConfName);
			ret.setCrawlAccepted(false);
			ret.setMessage("This job is a CSV job, use feedService");
			return ret;
			
		}else if (fetchersService.stats().getCrawlerStats().containsKey(datasourceConfName)) {
			logger.warn("Will skip crawl of {}, this job is already running",datasourceConfName);
			ret.setCrawlAccepted(false);
			ret.setMessage("This fetching is already running");
			return ret;
		}

//		if (fetchersService.stats().getCrawlerStats().size() >  config.getConcurrentFetcherTask() ) {
//			logger.warn("Cannot process crawl of {}, queue is full ({})",datasourceConfName,fetchersService.stats().getCrawlerStats().size());
//			ret.setCrawlAccepted(false);
//			ret.setMessage("Crawler queue is full");
//			return ret;
//		}

		logger.info("Starting crawl of {})",datasourceConfName);
		fetchersService.start(p,datasourceConfName);
		return ret;
	}

	@PostMapping(path=UrlConstants.CRAWLER_API_STOP_FETCHING)
	public FetchRequestResponse stop(@RequestBody final DataSourceProperties p) {

		final FetchRequestResponse ret = new FetchRequestResponse(true);

		if (!this.fetchersService.stats().getCrawlerStats().containsKey(p.getName())) {
			logger.warn("Cannot stop {}, this job does not exists",p.getName());
			ret.setCrawlAccepted(false);
			ret.setMessage("This fetching job does not exists");
			return ret;
		}

		logger.info("Stopping crawl of {})",p.getName());
		this.fetchersService.stop(p, p.getDatasourceConfigName());
		return ret;
	}

	@PostMapping(path=UrlConstants.CRAWLER_API_DIRECT_URL_REQUEST_FETCHING)
	public DataFragment fetchUrl(@RequestBody final DataSourceProperties p, @RequestParam final String url) throws TechnicalException, IOException, InterruptedException {
		return fetchersService.getWebDatasourceFetchingService().synchCrawl(p, url);
	}


	@GetMapping("/stats/webscrapers")
	public FetcherGlobalStats stats() {
		return fetchersService.stats();
	}




}
