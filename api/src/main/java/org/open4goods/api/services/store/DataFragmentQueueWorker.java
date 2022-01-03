package org.open4goods.api.services.store;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.open4goods.api.services.FullGenerationService;
import org.open4goods.dao.AggregatedDataRepository;
import org.open4goods.exceptions.AggregationSkipException;
import org.open4goods.model.data.DataFragment;
import org.open4goods.model.product.AggregatedData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * Worker thread that asynchronously dequeue the DataFragments from the file queue. It
 * @author goulven
 *
 */
public class DataFragmentQueueWorker implements Runnable {


	private static final Logger logger = LoggerFactory.getLogger(DataFragmentQueueWorker.class);

	/** The service used to "atomically" fetch and store / update DataFragments **/
	private final DataFragmentStoreService service;

	/** Size of pages that will be bulked to the DataFragmentStore**/
	private final int dequeuePageSize;

	/** The duration of the worker thread pause when nothing to get from the queue **/
	private final int pauseDuration;

	/**
	 * The service used for data generation
	 */
	private FullGenerationService generationService;
	
	/**
	 * The repository for aggregated data
	 */
	private AggregatedDataRepository aggregatedDataRepository;

	/**
	 * The worker name
	 */
	private final String workerName;


	/**
	 * Constructor
	 * @param owningService
	 * @param dequeuePageSize
	 */
	public DataFragmentQueueWorker(final DataFragmentStoreService owningService, final int dequeuePageSize, final int pauseDuration, String workerName, FullGenerationService generationService, AggregatedDataRepository aggregatedDataRepository) {
		service = owningService;
		this.dequeuePageSize = dequeuePageSize;
		this.pauseDuration = pauseDuration;
		this.workerName = workerName;
		this.generationService = generationService;
		this.aggregatedDataRepository = aggregatedDataRepository;
	}

	@Override
	public void run() {

		while (!service.getServiceShutdown().get()) {
			try {
				if (service.isDequeueEnabled().get() && !service.getFileQueue().isEmpty()) {
					// Store operation retrieve fragments, historize and re-index
					long now = System.currentTimeMillis();
					
					// There is data to consume and queue consummation is enabled
					final Set<DataFragment> buffer = service.dequeueMulti(dequeuePageSize);
					
					if (buffer.size() == 0 ) {
						logger.info("Empty datafragments buffer");
						continue;
					}
					
					// Retrieving datafragments
					Map<String, AggregatedData> aggDatas = aggregatedDataRepository.multiGetById(buffer.stream().map(e->e.gtin()).toList());
					
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
					
					
					logger.info("{} has indexed {} DataFragments in {}ms. {} Remaining in queue",workerName,  buffer.size(),System.currentTimeMillis()-now, service.getFileQueue().size());

				} else {
					try {
						logger.debug("No DataFragments to dequeue. Will sleep {}ms",pauseDuration);
						Thread.sleep(pauseDuration);
					} catch (final InterruptedException e) {
					}
				}
			} catch (final Exception e) {
				logger.error("Error while dequeing DataFragments",e);
			}
		}
	}
}