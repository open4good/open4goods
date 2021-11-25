package org.open4goods.api.services.store;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.PreDestroy;

import org.apache.commons.lang3.StringUtils;
import org.open4goods.api.services.FullGenerationService;
import org.open4goods.dao.AggregatedDataRepository;
import org.open4goods.exceptions.ValidationException;
import org.open4goods.model.Standardisable;
import org.open4goods.model.data.DataFragment;
import org.open4goods.services.SerialisationService;
import org.open4goods.services.StandardiserService;
import org.open4goods.store.repository.CustomDataFragmentRepository;
import org.open4goods.store.repository.DataFragmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

import com.bluejeans.bigqueue.BigQueue;

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

	// If false, dataFragments are store in the file queue but not sent to the
	// DataFragmentRepository
	private final AtomicBoolean dequeueEnabled = new AtomicBoolean(true);

	// Queue worker shutdown condition
	private final AtomicBoolean serviceShutdown = new AtomicBoolean(false);

	// The file queue implementation
	private final BigQueue fileQueue;


	
	/**
	 *
	 * @param queueFolder The folder where indexation queued datas will be stored
	 */
	public DataFragmentStoreService(StandardiserService standardiserService, SerialisationService serialisationService, DataFragmentRepository repository, final String queueFolder, final int dequeueSize, final int pauseDuration, final int workers, FullGenerationService generationService, AggregatedDataRepository aggregatedDataRepository) {

		this.pauseDuration=pauseDuration;
		this.repository = repository;
		this.serialisationService = serialisationService;
		this.standardiserService = standardiserService;
		
		logger.info("Creating/resuming a filequeue at {} ", queueFolder);
		fileQueue = new BigQueue(queueFolder, "DataFragments");

		logger.info("Starting file queue consumer thread, with bulk page size of {} items", dequeueSize);
	
		
		for (int i = 0; i < workers; i++) {			
			new Thread(new DataFragmentQueueWorker(this, dequeueSize, pauseDuration,"dequeue-worker-"+i, generationService, aggregatedDataRepository)).start();
		}

	}

	/**
	 * Cleans up the file space used by the filequeue
	 * 
	 */
	@Scheduled(initialDelay = 1000L, fixedRate = 1000* 3600 * 1)
	public void gc() {
		fileQueue.gc();		
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
		fileQueue.enqueue(serialisationService.toBytes(df));
	}

	/**
	 * Retrieves element from the persisted queue
	 *
	 * @param df
	 */
	Set<DataFragment> dequeueMulti(final int max) {
		final Set<DataFragment> ret = new HashSet<>();
		final List<byte[]> res = fileQueue.dequeueMulti(max);
		for (final byte[] r : res) {
			try {
				ret.add(serialisationService.fromBytes(r, DataFragment.class));
			} catch (final IOException e) {
				logger.error("Error wile deserializing",e );
			}
		}
		
		

		
		
		return ret;
	}

	public @PreDestroy void destroy() {
		serviceShutdown.set(true);
	}

	/////////////////////////////
	// Getters
	/////////////////////////////

	public AtomicBoolean isDequeueEnabled() {
		return dequeueEnabled;
	}

	public void enableDequeue(final boolean val) {
		dequeueEnabled.set(val);
	}

	public BigQueue getFileQueue() {
		return fileQueue;
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
