package org.open4goods.crawler.services.fetching;

import java.io.IOException;
import java.util.Map;

import org.open4goods.commons.config.yml.datasource.DataSourceProperties;
import org.open4goods.commons.helper.GenericFileLogger;
import org.open4goods.commons.model.crawlers.IndexationJobStat;
import org.open4goods.crawler.repository.IndexationRepository;
import org.open4goods.model.exceptions.TechnicalException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

import ch.qos.logback.classic.Level;

/**
 * Contracts for a datasource fetcher
 * 
 * @author Goulven.Furet
 *
 */
public abstract class DatasourceFetchingService {

	private static final Logger logger = LoggerFactory.getLogger(DatasourceFetchingService.class);

	protected @Autowired Environment env;

	private IndexationRepository indexationRepository;

	public DatasourceFetchingService(final String logsFolder, IndexationRepository indexationRepository) {
		this.indexationRepository = indexationRepository;
	}

	/**
	 * Start a Datasource fetching
	 * 
	 * @param provider
	 * @throws TechnicalException
	 * @throws InterruptedException
	 * @throws IOException
	 */
	public abstract void start(DataSourceProperties provide, String datasourceConfName) throws TechnicalException, IOException, InterruptedException;

	/**
	 * Stop a Datasource fetching
	 * 
	 * @param provider
	 */

	public abstract void stop(final String providerName);

	/**
	 * Provides stats about the current fetching running on this fetcher
	 * 
	 * @param provider
	 */

	public abstract Map<String, IndexationJobStat> stats();

	/**
	 * @param fetchingJobStats
	 * @param dataSourceProperties
	 * @param string
	 *
	 */
	public void finished(final IndexationJobStat fetchingJobStats, final DataSourceProperties dataSourceProperties) {

		// Logging the number of indexed and number
		logger.info("Datasource fetching of {} is terminated", dataSourceProperties.getName());

//		// Triggering an alert if indexed threshold not respected
//		if (fetchingJobStats.getNumberOfIndexedDatas() < dataSourceProperties.getMinimumIndexedItems()   ) {
//			dedicatedLogger.error(fetchingJobStats.getName() + " has indexed " + fetchingJobStats.getNumberOfIndexedDatas() + " datafragments, was expecting at least " + dataSourceProperties.getMinimumIndexedItems());
//		}

		// Logging to kibana
		if (null == indexationRepository) {
			logger.warn("Null indexationRepository, cannot save stats");
		} else {
			indexationRepository.save(fetchingJobStats);
		}

	}

	protected Logger createDatasourceLogger(final String datasourceConfigName,
			final DataSourceProperties datasourceConfig, final String logDir) {
		Level level = Level.toLevel(datasourceConfig.getDedicatedLogLevel(), Level.WARN);
		if ( Level.toLevel(datasourceConfig.getDedicatedLogLevel(), Level.OFF).equals(Level.OFF)) {
			logger.warn("Specific logging for {} is not or badly defined. Turned warn", datasourceConfigName);
			level = Level.toLevel(datasourceConfig.getDedicatedLogLevel(), Level.WARN);
		}

        final Logger dedicatedLogger = GenericFileLogger.initLogger(datasourceConfigName, level, logDir);
		return dedicatedLogger;
	}
}
