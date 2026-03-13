package org.open4goods.api.services.store;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang3.StringUtils;
import org.open4goods.api.services.AggregationFacadeService;
import org.open4goods.api.services.aggregation.services.realtime.PriceAggregationService;
import org.open4goods.api.dto.pricealert.InternalPriceEventDto;
import org.open4goods.api.services.pricealert.PriceAlertingService;
import org.open4goods.commons.exceptions.AggregationSkipException;
import org.open4goods.model.Standardisable;
import org.open4goods.model.StandardiserService;
import org.open4goods.model.datafragment.DataFragment;
import org.open4goods.model.exceptions.ValidationException;
import org.open4goods.model.price.AggregatedPrice;
import org.open4goods.model.product.Product;
import org.open4goods.model.product.ProductCondition;
import org.open4goods.model.product.ProductPartialUpdateHolder;
import org.open4goods.services.productrepository.config.IndexationConfig;
import org.open4goods.services.productrepository.services.ProductRepository;
import org.open4goods.services.productrepository.workers.FullProductIndexationWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.Striped;

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
	
	private final PriceAlertingService priceAlertingService;

	private final Striped<java.util.concurrent.locks.Lock> gtinLocks = Striped.lock(1024);


	/**
	 * 
	 * @param indexationConfig 
	 * @param queueFolder The folder where indexation queued datas will be stored
	 */
	public DataFragmentStoreService(StandardiserService standardiserService, AggregationFacadeService generationService, ProductRepository aggregatedDataRepository, IndexationConfig indexationConfig, PriceAlertingService priceAlertingService) {


		this.standardiserService = standardiserService;
		this.aggregatedDataRepository = aggregatedDataRepository;
		this.generationService=generationService;
		this.priceAlertingService = priceAlertingService;
		
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
			Map<String, List<DataFragment>> fragmentsByGtin = groupByGtin(buffer);
			if (fragmentsByGtin.isEmpty()) {
				return;
			}

			// Retrieving datafragments
			Map<String, Product> aggDatas = new LinkedHashMap<>(aggregatedDataRepository.multiGetById(
					fragmentsByGtin.keySet().stream()
					.map(Long::valueOf)
					.toList()));


			// Storing for partial updates
			Set<Product> fullItemsResults = new HashSet<Product>();
			// Storing for full updates
			Set<ProductPartialUpdateHolder> partialItemsResults = new HashSet<ProductPartialUpdateHolder>();
			List<InternalPriceEventDto> priceDropEvents = new ArrayList<>();
			
			
			for (Map.Entry<String, List<DataFragment>> entry : fragmentsByGtin.entrySet()) {
				handleGtinBuffer(entry.getKey(), entry.getValue(), aggDatas, fullItemsResults, partialItemsResults, priceDropEvents);
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

			priceAlertingService.publishPriceDropEvents(priceDropEvents);
			
		} catch (final Exception e) {
			logger.error("Error while dequeing DataFragments",e);
		}	
	}

	private void handleGtinBuffer(String gtin, List<DataFragment> fragments, Map<String, Product> aggDatas,
			Set<Product> fullItemsResults, Set<ProductPartialUpdateHolder> partialItemsResults,
			List<InternalPriceEventDto> priceDropEvents) {
		java.util.concurrent.locks.Lock lock = gtinLocks.get(gtin);
		lock.lock();
		try {
			Product data = aggDatas.get(gtin);
			if (null == data) {
				data = new Product();
				data.setCreationDate(System.currentTimeMillis());
				aggDatas.put(gtin, data);
			}

			EnumMap<ProductCondition, Double> beforePrices = snapshotBestPrices(data);
			boolean partialApplied = false;
			boolean fullApplied = false;

				for (DataFragment df : fragments) {
					Long hash = data.getDatasourceCodes().get(df.getDatasourceName());
					if (null != hash && hash.equals(Long.valueOf(df.getFragmentHashCode()))) {
						logger.info("Proceeding to partial update for {}", data.getId());
						try {
							priceService.onDataFragment(df, data, null);
							data.setLastChange(System.currentTimeMillis());
							partialApplied = true;
						} catch (AggregationSkipException e1) {
							logger.warn("Partial aggregation skipped for {} : {}", df, e1.getMessage());
						}
					} else {
					logger.info("Proceeding to full update for {}", data.getId());
					data.getDatasourceCodes().put(df.getDatasourceName(), Long.valueOf(df.getFragmentHashCode()));
					try {
						generationService.updateOne(df, data);
						fullApplied = true;
					} catch (AggregationSkipException e1) {
						logger.warn("Aggregation skipped for {} : {}", df, e1.getMessage());
					}
				}
			}

			if (fullApplied) {
				fullItemsResults.add(data);
			} else if (partialApplied && data.getId() != null) {
				ProductPartialUpdateHolder partial = new ProductPartialUpdateHolder(data.getId());
				partial.addChange("lastChange", data.getLastChange());
				partial.addChange("price", data.getPrice());
				partial.addChange("offersCount", data.getOffersCount());
				partialItemsResults.add(partial);
			}

			priceDropEvents.addAll(buildPriceDropEvents(gtin, beforePrices, snapshotBestPrices(data)));
		} finally {
			lock.unlock();
		}
	}

	private Map<String, List<DataFragment>> groupByGtin(Collection<DataFragment> buffer) {
		Map<String, List<DataFragment>> grouped = new LinkedHashMap<>();
		for (DataFragment fragment : buffer) {
			if (fragment == null || fragment.gtin() == null) {
				continue;
			}
			grouped.computeIfAbsent(fragment.gtin(), key -> new ArrayList<>()).add(fragment);
		}
		return grouped;
	}

	private EnumMap<ProductCondition, Double> snapshotBestPrices(Product product) {
		EnumMap<ProductCondition, Double> snapshot = new EnumMap<>(ProductCondition.class);
		snapshot.put(ProductCondition.NEW, bestPrice(product, ProductCondition.NEW));
		snapshot.put(ProductCondition.OCCASION, bestPrice(product, ProductCondition.OCCASION));
		return snapshot;
	}

	private Double bestPrice(Product product, ProductCondition condition) {
		AggregatedPrice offer = product.getPrice() == null ? null : product.getPrice().bestOffer(condition);
		return offer == null ? null : offer.getPrice();
	}

	private List<InternalPriceEventDto> buildPriceDropEvents(String gtin, EnumMap<ProductCondition, Double> beforePrices,
			EnumMap<ProductCondition, Double> afterPrices) {
		List<InternalPriceEventDto> events = new ArrayList<>();
		for (ProductCondition condition : ProductCondition.values()) {
			Double before = beforePrices.get(condition);
			Double after = afterPrices.get(condition);
			if (before != null && after != null && after < before) {
				events.add(new InternalPriceEventDto(Long.valueOf(gtin), condition, before, after, Instant.now()));
			}
		}
		return events;
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
