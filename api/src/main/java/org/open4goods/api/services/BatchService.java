package org.open4goods.api.services;

import java.util.concurrent.TimeUnit;

import org.open4goods.model.constants.TimeConstants;
import org.open4goods.model.crawlers.FetcherGlobalStats;
import org.open4goods.services.VerticalsConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

/**
 * One batch to rule them all
 * 
 * @author Goulven.Furet
 *
 *
 */
public class BatchService {

	protected static final Logger logger = LoggerFactory.getLogger(BatchService.class);

	// The status of the registered crawlers, which auto expires the crawlers if not
	// seen
	private Cache<String, FetcherGlobalStats> crawlerStatuses = CacheBuilder.newBuilder()
			.expireAfterWrite(TimeConstants.API_EXPIRED_UNSEEN_CRAWLERS_IN_SECONDS, TimeUnit.SECONDS).build();

	private VerticalsConfigService verticalsConfigService;

	private CompletionFacadeService completionFacadeService;

	private AggregationFacadeService aggregationFacadeService;

	public BatchService(AggregationFacadeService aggregationFacadeService,
			CompletionFacadeService completionFacadeService, VerticalsConfigService verticalsConfigService) {
		super();
		this.aggregationFacadeService = aggregationFacadeService;
		this.completionFacadeService = completionFacadeService;
		this.verticalsConfigService = verticalsConfigService;

	}

	@Scheduled(cron = "0 0 1,13 * * ?")
	// TODO : From conf
	public void batch() {

		logger.info("Starting batch");

		try {
			//  complete with icecat
			completionFacadeService.icecatCompletionAll();
			Thread.sleep(5000L);
		} catch (Exception e) {
			logger.error("Error in batch : icecat completion fail", e);
		}

		
		try {
			// Scoring
			aggregationFacadeService.scoreAll();
			Thread.sleep(5000L);
		} catch (Exception e) {
			logger.error("Error in batch : scoring fail", e);
		}

		
		
		try {
			//  resource complete
			completionFacadeService.resourceCompletionAll();
			Thread.sleep(5000L);
		} catch (Exception e) {
			logger.error("Error in batch : resource completion fail", e);
		}
		
		//  gen ai complete
		try {
			completionFacadeService.genaiCompletionAll();
			Thread.sleep(5000L);
		} catch (Exception e) {
			logger.error("Error in batch : genai completion fail", e);
		}
		
		try {
			//  amazon complete
			completionFacadeService.amazonCompletionAll();
		} catch (Exception e) {
			logger.error("Error in batch : amazon completion fail", e);
		}

		logger.info("End of batch");
	}

}
