package org.open4goods.api.services.store;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

import javax.annotation.PreDestroy;

import org.apache.commons.lang3.StringUtils;
import org.open4goods.api.services.RealtimeAggregationService;
import org.open4goods.dao.AggregatedDataRepository;
import org.open4goods.exceptions.AggregationSkipException;
import org.open4goods.exceptions.ValidationException;
import org.open4goods.model.Standardisable;
import org.open4goods.model.data.DataFragment;
import org.open4goods.model.product.AggregatedData;
import org.open4goods.services.SerialisationService;
import org.open4goods.services.StandardiserService;
import org.open4goods.store.repository.CustomDataFragmentRepository;
import org.open4goods.store.repository.DataFragmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

import io.micrometer.core.annotation.Timed;

/**
 * This service is in charge of DataFragments update and persistence. All
 * indexation request are queued in a file. This queue is asynchronously read by
 * {@link DataFragmentQueueWorker} in order to "bulk" and delay the
 * DataFragments update operations through {@link DataFragmentRepository} .<br/>
 *
 * It also provides mechanism to stop indexation (and keep data in the persisted
 * file), and to perform "direct" updates without giving up to the file buffer
 *
 * @author Goulven.Furet
 *
 */

public class DataFragmentStoreService {

	private static final Logger logger = LoggerFactory.getLogger(DataFragmentStoreService.class);

	private final DataFragmentRepository repository;
	private SerialisationService serialisationService;


	/**
	 * Duration of the pause the dequeuing thread will do if nothing to index in the queue
	 */
	private final Integer pauseDuration;

	public StandardiserService standardiserService;

//	// If false, dataFragments are store in the file queue but not sent to the
//	// DataFragmentRepository
//	private final AtomicBoolean dequeueEnabled = new AtomicBoolean(true);

	// Queue worker shutdown condition
	private final AtomicBoolean serviceShutdown = new AtomicBoolean(false);

	// The file queue implementation
	private final Map<String, DataFragment > fileQueue = new ConcurrentHashMap<>();

	private AggregatedDataRepository aggregatedDataRepository;

	private RealtimeAggregationService generationService;

	
	/**
	 *
	 * @param queueFolder The folder where indexation queued datas will be stored
	 */
	public DataFragmentStoreService(StandardiserService standardiserService, SerialisationService serialisationService, DataFragmentRepository repository, final String queueFolder, final int dequeueSize, final int pauseDuration, final int workers, RealtimeAggregationService generationService, AggregatedDataRepository aggregatedDataRepository) {

		this.pauseDuration=pauseDuration;
		this.repository = repository;
		this.serialisationService = serialisationService;
		this.standardiserService = standardiserService;
		this.aggregatedDataRepository = aggregatedDataRepository;
		this.generationService=generationService;
		
		logger.info("Creating/resuming a filequeue at {} ", queueFolder);
		

		logger.info("Starting file queue consumer thread, with bulk page size of {} items", dequeueSize);
	
	}

	/**
	 * Add multiple dataFragments to the indexing queue
	 *
	 * @param dataFragments
	 */
	public void queueDataFragments(final Set<DataFragment> dataFragments) {
		for (final DataFragment df : dataFragments) {
			queueDataFragment(df);

		}
	}

	/**
	 * Add an element to the indexing queue
	 *
	 * @param dataFragment
	 */
	@Timed(value = "queueDataFragment", description = "Validation, standardisation and addding to queue a DataFragment")
	public void queueDataFragment(final DataFragment data) {

		try {
			preHandle(data);
		} catch (final ValidationException e) {
			logger.warn("Cannot index data {} because of validation errors : {}", data.getUrl(), e.getMessage());
			return;
		}

		logger.info("Queuing datafragment {}",data);

		enqueue(data);
	}

	/**
	 * @param data
	 */
	private void preHandle(final DataFragment data) throws ValidationException{

		/////////////////////////////////////////
		// Validating
		/////////////////////////////////////////
		
		if (!StringUtils.isNumeric(data.gtin())) {
			//TODO : come back on standard validation model
			throw new ValidationException("No gtin");
		}
		data.validate();

		////////////////////////////////////////////////////////
		// Standardisation (currencies, ratings scales, ...)
		////////////////////////////////////////////////////////


		for (final Standardisable s : data.standardisableChildren()) {
			s.standardize(standardiserService, StandardiserService.DEFAULT_CURRENCY);
		}

		//////////////////////////////////////////////////////////
		// Providing it to the store service (favicon / logo update)
		//////////////////////////////////////////////////////////

		//TODO(gof) : handle store icons other where
		//		storeService.updateStoreIcon(data);
	}


	//	public void indexNow(Set<DataFragment> dataFragments) {
	//
	//	}
	//
	//	public void indexNow(DataFragment dataFragment) {
	//
	//	}

	/**
	 * Add an element to the persisted queue
	 *
	 * @param df
	 */
	void enqueue(final DataFragment df) {
		
		
		
		fileQueue.put(df.getUrl(),df);
		
		// Trigger hard indexing 
		//TODO(conf) : elastinc bulk size from conf
		
		
		if (fileQueue.size() > 400) {
			aggregateAndstore();
		}
	}


	/**
	 * Aggregates datafragments to already known aggregatedDatas, then store the results
	 * Scheduled evey hour to flush buffer
	 */
	
	@Scheduled( fixedDelay = 3600 * 1000)
	public void aggregateAndstore() {
		
		try {
			
				if (fileQueue.isEmpty()) {
					logger.info("No datafragments to index");
					return;
				}
			
				logger.info("Aggregating {} items",fileQueue.size());
				// Store operation retrieve fragments, historize and re-index
				long now = System.currentTimeMillis();
				
				// There is data to consume and queue consummation is enabled
				final Collection<DataFragment> buffer = fileQueue.values();
		
				
				
				// Retrieving datafragments
				Map<String, AggregatedData> aggDatas = aggregatedDataRepository.multiGetById(
							
						buffer.stream()
						.filter(s -> StringUtils.isNotBlank(s.gtin()))
						.map(e->e.gtin()).toList());

				
				// Aggregating to product datas
				Set<AggregatedData> results = new HashSet<AggregatedData>();
				
				for (DataFragment df : buffer) {
					AggregatedData data = aggDatas.get(df.gtin());						
					if (null == data) {
						data = new AggregatedData();
						data.setCreationDate(System.currentTimeMillis());
					}
					
					try {

						results.add(generationService.process(df,data));
					} catch (AggregationSkipException e1) {
						logger.warn("Aggregation skipped for {} : {}",df,e1.getMessage());
					}
					
				}
				

				
				
				// Saving the result
				aggregatedDataRepository.index(results);
				
				
				logger.info("Indexed {} DataFragments in {}ms.",  buffer.size(),System.currentTimeMillis()-now);

				// Clearing queue
				fileQueue.clear();
				
				
			
		} catch (final Exception e) {
			logger.error("Error while dequeing DataFragments",e);
		}
	
		
	}
	
	
	
	
	
	
	
	
	
	public @PreDestroy void destroy() {
		serviceShutdown.set(true);
	}



	public CustomDataFragmentRepository getRepository() {
		return repository;
	}

	public AtomicBoolean getServiceShutdown() {
		return serviceShutdown;
	}

	public Integer getPauseDuration() {
		return pauseDuration;
	}

}
