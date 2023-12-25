package org.open4goods.api.services.store;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang3.StringUtils;
import org.open4goods.api.services.RealtimeAggregationService;
import org.open4goods.dao.ProductRepository;
import org.open4goods.exceptions.AggregationSkipException;
import org.open4goods.exceptions.ValidationException;
import org.open4goods.model.Standardisable;
import org.open4goods.model.data.DataFragment;
import org.open4goods.model.product.Product;
import org.open4goods.services.StandardiserService;
import org.open4goods.store.repository.ProductIndexationWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

import jakarta.annotation.PreDestroy;

/**
 * This service is in charge of DataFragments update and persistence. All
 * indexation request are queued in a file. This queue is asynchronously read by
 * {@link ProductIndexationWorker} in order to "bulk" and delay the
 * DataFragments update operations through {@link DataFragmentRepository} .<br/>
 *
 * It also provides mechanism to stop indexation (and keep data in the persisted
 * file), and to perform "direct" updates without giving up to the file buffer
 * TODO : Could also have a thread pool here to increase performances
 * @author Goulven.Furet
 *
 */

public class DataFragmentStoreService {

	// TODO : from conf
	private static final int DATAFRAGMENT_INDEXATION_BULK_SIZE = 250;


	private static final Logger logger = LoggerFactory.getLogger(DataFragmentStoreService.class);


	public StandardiserService standardiserService;

	//	// If false, dataFragments are store in the file queue but not sent to the
	//	// DataFragmentRepository
	//	private final AtomicBoolean dequeueEnabled = new AtomicBoolean(true);

	// Queue worker shutdown condition
	private final AtomicBoolean serviceShutdown = new AtomicBoolean(false);

	// The queue implementation
	// TODO : Limit from conf
	private BlockingQueue<DataFragment> queue = new LinkedBlockingQueue<>(3000000);

	private ProductRepository aggregatedDataRepository;

	private RealtimeAggregationService generationService;


	/**
	 *
	 * @param queueFolder The folder where indexation queued datas will be stored
	 */
	public DataFragmentStoreService(StandardiserService standardiserService, RealtimeAggregationService generationService, ProductRepository aggregatedDataRepository) {


		this.standardiserService = standardiserService;
		this.aggregatedDataRepository = aggregatedDataRepository;
		this.generationService=generationService;

//		int dequeueSize = 200;
//		int workers = 4;
//		int pauseDuration = 1000;
//		
//		logger.info("Starting file queue consumer thread, with bulk page size of {} items", dequeueSize );
//				
//		for (int i = 0; i < workers; i++) {			
//			new Thread(new ProductIndexationWorker(this, dequeueSize, pauseDuration,"dequeue-worker-"+i)).start();
//		}
		
		

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
			queueDataFragment(df);

		}
	}

	/**
	 * Add an element to the indexing queue
	 *
	 * @param dataFragment
	 */
//	@Timed(value = "queueDataFragment", description = "Validation, standardisation and addding to queue a DataFragment")
	public void queueDataFragment(final DataFragment data) {

		try {
			preHandle(data);
		} catch (final ValidationException e) {
			logger.info("Cannot index data {} because of validation errors : {}", data.getUrl(), e.getMessage());
			return;
		}

		logger.debug("Queuing datafragment {}",data);

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

	}




	/**
	 * Add an element to the persisted queue
	 *
	 * @param df
	 */
	void enqueue(final DataFragment df) {
		queue.add(df);
		if (queue.size() > DATAFRAGMENT_INDEXATION_BULK_SIZE) {
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

			if (queue.isEmpty()) {
				logger.info("No datafragments to index");
				return;
			}

			logger.info("Aggregating {} items",queue.size());
			// Store operation retrieve fragments, historize and re-index
			long now = System.currentTimeMillis();

			// There is data to consume and queue consummation is enabled
			logger.info("Dequeuing {} datafragments", queue.size());			
			final Collection<DataFragment> buffer = new ArrayList<>();
			
			for (int i = 0; i < DATAFRAGMENT_INDEXATION_BULK_SIZE; i++) {
				buffer.add(queue.take());
			}

			// Retrieving datafragments
			Map<String, Product> aggDatas = aggregatedDataRepository.multiGetById(

					buffer.stream()
					.map(DataFragment::gtin)
					.filter(StringUtils::isNotBlank).toList());


			// Aggregating to product datas
			Set<Product> results = new HashSet<Product>();

			for (DataFragment df : buffer) {
				Product data = aggDatas.get(df.gtin());
				if (null == data) {
					data = new Product();
					data.setCreationDate(System.currentTimeMillis());
				}

				try {
					// TODO : Not the good point. Service ?
					results.add(generationService.process(df,data));
				} catch (AggregationSkipException e1) {
					logger.info("Aggregation skipped for {} : {}",df,e1.getMessage());
				}

			}




			// Saving the result
			aggregatedDataRepository.index(results);


			logger.info("Indexed {} DataFragments in {}ms.",  buffer.size(),System.currentTimeMillis()-now);

		} catch (final Exception e) {
			logger.error("Error while dequeing DataFragments",e);
		}

		// Clearing queue
		queue.clear();

	}




	public AtomicBoolean getServiceShutdown() {
		return serviceShutdown;
	}





}
