package org.open4goods.crawler.services.fetching;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.open4goods.config.yml.datasource.DataSourceProperties;
import org.open4goods.crawler.config.yml.FetcherProperties;
import org.open4goods.crawler.services.AbstractAggregatedDataWorker;
import org.open4goods.crawler.services.DataFragmentCompletionService;
import org.open4goods.crawler.services.IndexationService;
import org.open4goods.model.crawlers.FetchingJobStats;
import org.open4goods.services.SerialisationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

/**
 * Service that handles the AggregatedData backed services
 *
 * @author goulven
 *
 */

public class AggregatedDataBackedFetchingService extends DatasourceFetchingService{


	private static final int CONCURRENT_API_FETCHING_JOBS = 5;

	private static final Logger logger = LoggerFactory.getLogger(AggregatedDataBackedFetchingService.class);

	private final Map<String, AbstractAggregatedDataWorker> workers = new ConcurrentHashMap<>();

	private @Autowired ApplicationContext applicationContext;

	private final SerialisationService serialisationService;

	private final IndexationService indexationService;

	private final DataFragmentCompletionService completionService;

	private final ExecutorService executor;

	private final String logsFolder;

	// The running job status
	private final Map<String, FetchingJobStats> running = new ConcurrentHashMap<>();

	private final FetcherProperties fetcherProperties;

	public AggregatedDataBackedFetchingService(final IndexationService indexationService, final String logsFolder, final DataFragmentCompletionService completionService,  final SerialisationService serialisationService, final FetcherProperties fetcherProperties) {
		super(logsFolder);
		this.logsFolder=logsFolder;
		this.indexationService = indexationService;

		executor = Executors.newFixedThreadPool(CONCURRENT_API_FETCHING_JOBS);
		this.completionService = completionService;
		this.serialisationService = serialisationService;
		this.fetcherProperties = fetcherProperties;
	}


	@Override
	public void start(final DataSourceProperties provider, final String datasourceConfName) {

		final Logger dedicatedLogger = createDatasourceLogger(datasourceConfName, provider,logsFolder);

		dedicatedLogger.info("dedicated logging started for {}",datasourceConfName);



		if (running.containsKey(datasourceConfName)) {
			logger.error("Job {} is already running",provider);
			return;
		}

		try {
			// Hot instanciation of the API fetching class
			final Class<? extends AbstractAggregatedDataWorker> workerClass = (Class<? extends AbstractAggregatedDataWorker>) Class.forName(provider.getApiDatasource().getAggregatedDataBackingClass());
			applicationContext.getAutowireCapableBeanFactory().autowireBean(workerClass);

			final AbstractAggregatedDataWorker worker = workerClass.newInstance();

			final FetchingJobStats stat = new FetchingJobStats(datasourceConfName, System.currentTimeMillis());

			// Call to init method
			worker.init(provider,datasourceConfName,this,stat, dedicatedLogger );

			// Puting in monitorings map
			running.put(datasourceConfName, stat);
			workers.put(datasourceConfName, worker);
			executor.submit(new Thread(worker));

		} catch (final Exception e) {
			logger.error("Error starting job {}",e);
			running.remove(datasourceConfName);
		}

	}


	@Override
	public void stop(final String providerName) {
		workers.get(providerName).stop();
	}


	/**
	 * Return the stats
	 */
	@Override
	public Map<String, FetchingJobStats> stats() {
		return running;
	}


	public SerialisationService getSerialisationService() {
		return serialisationService;
	}


	public IndexationService getIndexationService() {
		return indexationService;
	}


	public DataFragmentCompletionService getCompletionService() {
		return completionService;
	}


	public Map<String, FetchingJobStats> getRunning() {
		return running;
	}








}
