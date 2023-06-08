package org.open4goods.crawler.services.fetching;

import java.util.Map;

import org.apache.commons.lang3.ArrayUtils;
import org.open4goods.config.yml.datasource.DataSourceProperties;
import org.open4goods.exceptions.TechnicalException;
import org.open4goods.helper.GenericFileLogger;
import org.open4goods.model.crawlers.FetchingJobStats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

import ch.qos.logback.classic.Level;

/**
 * Contracts for a datasource fetcher
 * @author Goulven.Furet
 *
 */
public abstract class DatasourceFetchingService {

	private static final Logger logger = LoggerFactory.getLogger(DatasourceFetchingService.class);


	protected @Autowired Environment env;

	protected Logger dedicatedLogger;

	public DatasourceFetchingService (final String logsFolder, boolean toConsole) {
		dedicatedLogger = GenericFileLogger.initLogger("stats-datasource", Level.INFO, logsFolder, toConsole);
	}

	/**
	 * Start a Datasource fetching
	 * @param provider
	 * @throws TechnicalException 
	 */
	public abstract void start(DataSourceProperties provide,String datasourceConfName) throws TechnicalException;

	/**
	 * Stop a Datasource fetching
	 * @param provider
	 */

	public abstract void stop(final String providerName);

	/**
	 * Provides stats about the current fetching running on this fetcher
	 * @param provider
	 */

	public abstract  Map<String, FetchingJobStats> stats();


	/**
	 * @param fetchingJobStats
	 * @param dataSourceProperties
	 * @param string
	 *
	 */
	public void finished(final FetchingJobStats fetchingJobStats, final DataSourceProperties dataSourceProperties) {

		// Logging the number of indexed and number
		dedicatedLogger.info("Datasource fetching of {} is terminated, with a duration of {}ms. indexed:{}",dataSourceProperties.getName(),  System.currentTimeMillis() -  fetchingJobStats.getStartDate(), fetchingJobStats.getNumberOfIndexedDatas());


		// Triggering an alert if indexed threshold not respected
		if (fetchingJobStats.getNumberOfIndexedDatas() < dataSourceProperties.getMinimumIndexedItems()   ) {
			dedicatedLogger.error(fetchingJobStats.getName() + " has indexed " + fetchingJobStats.getNumberOfIndexedDatas() + " datafragments, was expecting at least " + dataSourceProperties.getMinimumIndexedItems());
		}
	}


	protected Logger createDatasourceLogger(final String datasourceConfigName,
			final DataSourceProperties datasourceConfig, final String logDir) {
		Level level = Level.toLevel(datasourceConfig.getDedicatedLogLevel(), Level.WARN);
		if ( Level.toLevel(datasourceConfig.getDedicatedLogLevel(), Level.OFF).equals(Level.OFF)) {
			logger.warn("Specific logging for {} is not or badly defined. Turned warn", datasourceConfigName);
			level = Level.toLevel(datasourceConfig.getDedicatedLogLevel(), Level.WARN);
		}

		// Logging to console according to dev profile and conf
		boolean toConsole = false;
		// TODO : Not nice, mutualize
		if (ArrayUtils.contains(env.getActiveProfiles(), "dev") || ArrayUtils.contains(env.getActiveProfiles(), "devsec")) {
			toConsole = true;
		}

		final Logger dedicatedLogger = GenericFileLogger.initLogger(datasourceConfigName, level, logDir,
				toConsole);
		return dedicatedLogger;
	}
}
