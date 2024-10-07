package org.open4goods.api.services.store;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang3.StringUtils;
import org.open4goods.api.services.AggregationFacadeService;
import org.open4goods.commons.config.yml.IndexationConfig;
import org.open4goods.commons.dao.ProductRepository;
import org.open4goods.commons.exceptions.AggregationSkipException;
import org.open4goods.commons.exceptions.ValidationException;
import org.open4goods.commons.model.Standardisable;
import org.open4goods.commons.model.data.DataFragment;
import org.open4goods.commons.model.product.Product;
import org.open4goods.commons.services.StandardiserService;
import org.open4goods.commons.store.repository.ProductIndexationWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.query.UpdateQuery;

import jakarta.annotation.PreDestroy;

/**
 * This service is in charge of DataFragments update and persistence. All
 * indexation request are queued in a file. This queue is asynchronously read by
 * {@link ProductIndexationWorker} in order to "bulk" and delay the
 * DataFragments update operations through {@link DataFragmentRepository} .<br/>
 *
 * It also provides mechanism to stop indexation (and keep data in the persisted
 * file), and to perform "direct" updates without giving up to the file buffer
 * @author Goulven.Furet
 *
 */

public class DataFragmentStoreService {

	private static final Logger logger = LoggerFactory.getLogger(DataFragmentStoreService.class);


	public StandardiserService standardiserService;

	//	// If false, dataFragments are store in the file queue but not sent to the
	//	// DataFragmentRepository
	//	private final AtomicBoolean dequeueEnabled = new AtomicBoolean(true);

	// Queue worker shutdown condition
	private final AtomicBoolean serviceShutdown = new AtomicBoolean(false);

	// The queue implementation
	private BlockingQueue<DataFragment> queue;

	private ProductRepository aggregatedDataRepository;

	private AggregationFacadeService generationService;


	/**
	 *
	 * @param indexationConfig 
	 * @param queueFolder The folder where indexation queued datas will be stored
	 */
	public DataFragmentStoreService(StandardiserService standardiserService, AggregationFacadeService generationService, ProductRepository aggregatedDataRepository, IndexationConfig indexationConfig) {


		this.standardiserService = standardiserService;
		this.aggregatedDataRepository = aggregatedDataRepository;
		this.generationService=generationService;

		this.queue =  new LinkedBlockingQueue<>(indexationConfig.getDatafragmentQueueMaxSize());
		
		for (int i = 0; i < indexationConfig.getDataFragmentworkers(); i++) {					
			logger.info("Starting file queue consumer thread {}, with bulk page size of {} items",i, indexationConfig.getDataFragmentBulkPageSize() );
			//TODO(p3,perf) : Virtual threads, but ko with visualVM profiling
			new Thread(new DataFragmentAggregationWorker(this, indexationConfig.getDataFragmentBulkPageSize(), indexationConfig.getPauseDuration(),"aggreg-worker-"+i)).start();;
		}
		
		

	}

	
	public @PreDestroy void destroy() {
		serviceShutdown.set(true);
	}
	
	
	/**
	 * Add multiple dataFragments to the indexing queue
	 *
	 * @param dataFragments
	 */
	public void queueDataFragments(final Set<DataFragment> dataFragments) {
		for (final DataFragment df : dataFragments) {
			try {
				queueDataFragment(df);
			} catch (ValidationException e) {
				logger.warn("Cannot add {} because of validation errors : {}",df,e.getMessage());
			}
		}
	}

	/**
	 * Add an element to the indexing queue
	 *
	 * @param dataFragment
	 * @throws ValidationException 
	 */
//	@Timed(value = "queueDataFragment", description = "Validation, standardisation and addding to queue a DataFragment")
	public void queueDataFragment(final DataFragment data) throws ValidationException {

		if (data.isBrandFragment()) {
			logger.debug("Skipping brand fragment classical indexation {}",data);
		} else {
			preHandle(data);
			logger.debug("Queuing datafragment {}",data);
			
			enqueue(data);
			
		}
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

	}




	/**
	 * Add an element to the persisted queue
	 *
	 * @param df
	 */
	void enqueue(final DataFragment df) {
		try {
			queue.put(df);
		} catch (Exception e) {
			logger.error("Exception while adding in the queue, ",e);			
		}	
	}


	/**
	 * Aggregates datafragments to already known aggregatedDatas, then store the results
	 */
	public void aggregateAndstore(Collection<DataFragment> buffer) {
		
		try {
			// Retrieving datafragments
			Map<String, Product> aggDatas = aggregatedDataRepository.multiGetById(

					buffer.stream()
					.map(e -> Long.valueOf(e.gtin()))
					.toList());


			// Aggregating to product datas
			Set<Product> results = new HashSet<Product>();

			for (DataFragment df : buffer) {
				Product data = aggDatas.get(df.gtin());
				if (null == data) {
					// This is a first product
					data = new Product();
					data.setCreationDate(System.currentTimeMillis());
				}

				// PRoceeding to aggregation pipeline
				try {
					results.add(generationService.updateOne(df,data));
				} catch (AggregationSkipException e1) {
					logger.warn("Aggregation skipped for {} : {}",df,e1.getMessage());
				}

			}

			// Saving the result
			aggregatedDataRepository.index(results);


			logger.warn("Indexed {} DataFragments. Queue size is {}",  buffer.size(),queue.size());

		} catch (final Exception e) {
			logger.error("Error while dequeing DataFragments",e);
		}	
	}


	public AtomicBoolean getServiceShutdown() {
		return serviceShutdown;
	}


	public BlockingQueue<DataFragment> getQueue() {
		return queue;
	}


	public void setQueue(BlockingQueue<DataFragment> queue) {
		this.queue = queue;
	}






}
