
package org.open4goods.crawler.controller;

import org.open4goods.config.yml.datasource.DataSourceProperties;
import org.open4goods.crawler.config.yml.FetcherProperties;
import org.open4goods.crawler.services.FetchersService;
import org.open4goods.exceptions.TechnicalException;
import org.open4goods.model.constants.UrlConstants;
import org.open4goods.model.crawlers.FetcherGlobalStats;
import org.open4goods.model.data.DataFragment;
import org.open4goods.model.dto.FetchRequestResponse;
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

		if (fetchersService.stats().getCrawlerStats().containsKey(datasourceConfName)) {
			logger.warn("Will skip crawl of {}, this job is already running",datasourceConfName);
			ret.setCrawlAccepted(false);
			ret.setMessage("This fetching is already running");
			return ret;
		}

		if (fetchersService.stats().getCrawlerStats().size() >  config.getConcurrentFetcherTask() ) {
			logger.warn("Cannot process crawl of {}, queue is full ({})",datasourceConfName,fetchersService.stats().getCrawlerStats().size());
			ret.setCrawlAccepted(false);
			ret.setMessage("Crawler queue is full");
			return ret;
		}

		logger.info("Starting crawl of {})",datasourceConfName);
		fetchersService.start(p,datasourceConfName);
		return ret;
	}

//	@PostMapping(path=UrlConstants.CRAWLER_API_STOP_FETCHING)
//	public FetchRequestResponse stop(@RequestBody final DataSourceProperties p) {
//
//		final FetchRequestResponse ret = new FetchRequestResponse(true);
//
//		if (!this.fetchersService.stats().getCrawlerStats().containsKey(p.getName())) {
//			logger.warn("Cannot stop {}, this job does not exists",p.getName());
//			ret.setCrawlAccepted(false);
//			ret.setMessage("This fetching job does not exists");
//			return ret;
//		}
//
//		logger.info("Stopping crawl of {})",p.getName());
//		this.fetchersService.stop(p);
//		return ret;
//	}

	@PostMapping(path=UrlConstants.CRAWLER_API_DIRECT_URL_REQUEST_FETCHING)
	public DataFragment fetchUrl(@RequestBody final DataSourceProperties p, @RequestParam final String url) throws TechnicalException {
		return fetchersService.getWebDatasourceFetchingService().synchCrawl(p, url);
	}

//	@PostMapping(path=UrlConstants.CRAWLER_API_DIRECT_CSV_REQUEST_FETCHING)
//	public DataFragment fetchCsv(@RequestBody final DataSourceProperties p, @RequestParam final String csvLine,@RequestParam final String csvHeaders) {
//		try {
//			return this.fetchersService.getCsvDatasourceFetchingService().synchFetch(p, csvHeaders, csvLine);
//		} catch (IOException | ValidationException e) {
//			logger.error("Error while drect csv line fecthing",e);
//			return null;
//		}
//	}


	@GetMapping("/stats/fetchersService")
	public FetcherGlobalStats stats() {
		return fetchersService.stats();
	}




}
