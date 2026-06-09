package org.open4goods.api.services.store;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang3.StringUtils;
import org.open4goods.api.services.AggregationFacadeService;
import org.open4goods.api.services.aggregation.services.realtime.PriceAggregationService;
import org.open4goods.commons.exceptions.AggregationSkipException;
import org.open4goods.model.Standardisable;
import org.open4goods.model.StandardiserService;
import org.open4goods.model.datafragment.DataFragment;
import org.open4goods.model.exceptions.ValidationException;
import org.open4goods.model.product.Product;
import org.open4goods.model.product.ProductPartialUpdateField;
import org.open4goods.model.product.ProductPartialUpdateHolder;
import org.open4goods.services.productrepository.config.IndexationConfig;
import org.open4goods.services.productrepository.services.ProductRepository;
import org.open4goods.services.productrepository.workers.FullProductIndexationWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.annotation.PreDestroy;

/**
 * This service is in charge of DataFragments update and persistence. All
 * indexation request are queued in a file. This queue is asynchronously read by
 * {@link FullProductIndexationWorker} in order to "bulk" and delay the
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

	PriceAggregationService priceService = new PriceAggregationService(logger);


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
			new Thread(new DataFragmentAggregationWorker(this, indexationConfig.getDataFragmentBulkPageSize(), indexationConfig.getPauseDuration(),"datafragment-worker-"+i)).start();;
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
	 * Add an element to the indexing queue.
	 *
	 * <p>Returns silently (without throwing) when the fragment lacks a numeric GTIN or is a brand
	 * fragment — these are the most common rejection reasons and using exceptions for control flow
	 * there caused significant stack-trace allocation overhead under high-throughput ingestion.
	 *
	 * @param data the fragment to enqueue
	 * @throws ValidationException when the fragment fails deeper validation in {@link #preHandle}
	 */
//	@Timed(value = "queueDataFragment", description = "Validation, standardisation and addding to queue a DataFragment")
	public void queueDataFragment(final DataFragment data) throws ValidationException {

		if (data.isBrandFragment()) {
			logger.debug("Skipping brand fragment classical indexation {}", data);
			return;
		}

		// Fast-path rejection: fragments without a numeric GTIN are extremely common
		// (e.g. rows without an EAN in the feed). Using a guard return instead of
		// throwing ValidationException avoids stack-trace allocation for each such row.
		if (!StringUtils.isNumeric(data.gtin())) {
			logger.debug("Skipping fragment with no numeric gtin: {}", data);
			return;
		}

		preHandle(data);
		logger.debug("Queuing datafragment {}", data);
		enqueue(data);
	}

	/**
	 * Validates and standardises a data fragment before it enters the queue.
	 *
	 * <p>Note: the GTIN numeric check has been moved to the caller ({@link #queueDataFragment})
	 * to avoid expensive exception construction for the common no-GTIN case.
	 *
	 * @param data the fragment to validate
	 * @throws ValidationException when the fragment fails validation
	 */
	private void preHandle(final DataFragment data) throws ValidationException {

		/////////////////////////////////////////
		// Validating
		/////////////////////////////////////////

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


			// Storing for partial updates
			Set<Product> fullItemsResults = new HashSet<Product>();
			// Storing for full updates
			Set<ProductPartialUpdateHolder> partialItemsResults = new HashSet<ProductPartialUpdateHolder>();
			
			
			for (DataFragment df : buffer) {
				Product data = aggDatas.get(df.gtin());
				if (null == data) {
					// This is a first product
					data = new Product();
					data.setCreationDate(System.currentTimeMillis());
				}

				/////////////////////////////
				// Updating the datasources
				/////////////////////////////
				
				// Fastening, by checking if exaclty same datafragment than previously
				Long hash = data.getDatasourceCodes().get(df.getDatasourceName());				
				if (null != hash && hash.equals(Long.valueOf(df.getFragmentHashCode()))) {
					// We can proceed to partial update, we just update lasttimechange and prices
					logger.info("Proceeding to partial update for {}",data.getId());
					// Updating price
					priceService.onDataFragment(df, data, null);
					// Updating lastChange
					data.setLastChange(System.currentTimeMillis());
					
					ProductPartialUpdateHolder partial = new ProductPartialUpdateHolder(data.getId());
					partial.addChange(ProductPartialUpdateField.LAST_CHANGE, data.getLastChange());
					partial.addChange(ProductPartialUpdateField.PRICE, data.getPrice());
					partial.addChange(ProductPartialUpdateField.OFFERS_COUNT, data.getOffersCount());
					
					partialItemsResults.add(partial);
					
				} else {
					// Item has changed, we proceed to full update
					logger.info("Proceeding to full update for {}",data.getId());
					data.getDatasourceCodes().put(df.getDatasourceName(), Long.valueOf(df.getFragmentHashCode()));
					
					
					// Proceeding to aggregation pipeline
					try {
						Product newP = generationService.updateOne(df,data);
						
						fullItemsResults.add(newP);
					} catch (AggregationSkipException e1) {
						logger.warn("Aggregation skipped for {} : {}",df,e1.getMessage());
					}
				}
			}

			// Saving the result
			
			if (fullItemsResults.size() > 0) {
				logger.info("Will submit {} full products for indexation (datafragment queue size is now {})",  fullItemsResults.size(),queue.size());
				aggregatedDataRepository.addToFullindexationQueue(fullItemsResults);
				logger.info("Submitted {} full products for indexation (datafragment queue size is now {})",  fullItemsResults.size(),queue.size());
			}
			
			
			if (partialItemsResults.size() > 0) {
				logger.info("Will submit {} partial products for indexation (datafragment queue size is now {})", partialItemsResults.size(), queue.size());
				aggregatedDataRepository.addToPartialIndexationQueue(partialItemsResults);
				logger.info("Submitted {} partial products for indexation (datafragment queue size is now {})", partialItemsResults.size(), queue.size());
			}
			
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
