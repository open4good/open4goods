package org.open4goods.api.services;

import java.util.Set;
import java.util.stream.Collectors;

import org.open4goods.aggregation.aggregator.BatchedAggregator;
import org.open4goods.api.config.yml.ApiProperties;
import org.open4goods.config.yml.ui.VerticalConfig;
import org.open4goods.dao.AggregatedDataRepository;
import org.open4goods.exceptions.AggregationSkipException;
import org.open4goods.helper.GenericFileLogger;
import org.open4goods.model.product.AggregatedData;
import org.open4goods.services.VerticalsConfigService;
import org.open4goods.store.repository.DataFragmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

import ch.qos.logback.classic.Level;

/**
 * This service is in charge of various batches
 * @author Goulven.Furet
 */
public class BatchService {



	private static final Logger logger = LoggerFactory.getLogger(BatchService.class);


	private DataFragmentRepository dataFragmentsRepository;

	private AggregatedDataRepository dataRepository;
	
	private VerticalsConfigService verticalsService;
	
	private BatchAggregationService batchAggregationService;
	
	

	private final ApiProperties apiProperties;

	private Logger dedicatedLogger;

	public BatchService( final DataFragmentRepository dataFragmentsRepository,
								AggregatedDataRepository dataRepository,
								ApiProperties apiProperties,
								VerticalsConfigService verticalsService,
								BatchAggregationService batchAggregationService) {
		super();
		
		dedicatedLogger = GenericFileLogger.initLogger("stats-batch", Level.INFO, apiProperties.logsFolder(), false);		
		this.dataFragmentsRepository = dataFragmentsRepository;
		this.apiProperties = apiProperties;
		this.dataRepository =dataRepository;
		this.verticalsService=verticalsService;
		this.batchAggregationService=batchAggregationService;
	}


	
	
	
	/**
	 * Update verticals with the batch Aggragator
	 */
	//TODO : cron schedule, at night
	@Scheduled( initialDelay = 1000 * 3600*24, fixedDelay = 1000 * 3600*24)
	public void scoreVertical() {
		

		
		for (VerticalConfig vertical : verticalsService.getConfigsWithoutDefault()) {
			BatchedAggregator agg = batchAggregationService.getAggregator(vertical);
			
			// Warning, full vertical load
			// TODO : Max from conf
			
			Set<AggregatedData> datas = dataRepository.exportVertical(vertical.getId(),1000000).collect(Collectors.toSet());
			
			agg.beforeStart(datas);

			for (AggregatedData data : datas) {
				
				try {
					AggregatedData updated = agg.update(data,datas);

					//TODO : bulk for performance
					dataRepository.index(updated);
				} catch (AggregationSkipException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					continue;
				}				
			}
			
			agg.close(datas);
			
		}
		
		
		
		
		
	}
	
	
	
	/**
	 * The batch used to associate verticals on AggregatedDatas based on categories
	 */
	public void definesVertical() {
		
		
		dedicatedLogger.info("Starting batch verticalisation");
		dataRepository.exportAll().forEach(e -> {
		
			// Getting the config for the category, if any

				VerticalConfig vConf = verticalsService.getVerticalForCategories(e.getDatasourceCategories());
				
				if (null != vConf) {
					// We have a match. Associate vertical ID annd save
					e.setVertical(vConf.getId());
					
					// Index
					//TODO : Bulk index for performance
					dedicatedLogger.warn("Vertical {} for vertical {}", vConf.getId() , e.bestName());
					dataRepository.index(e);

					
				} else if (null != e.getVertical() ){
					dedicatedLogger.warn("Nulling Vertical for {} ", e.bestName());
					e.setVertical(null);
					//TODO (gof) : bulkindex
					dataRepository.index(e);
				}
				
		});
		dedicatedLogger.info("End batch verticalisation");		
		
	}
	

	
	
	

}
