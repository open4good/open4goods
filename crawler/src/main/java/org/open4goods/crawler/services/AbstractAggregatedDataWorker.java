package org.open4goods.crawler.services;

import java.net.URL;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.apache.commons.io.IOUtils;
import org.open4goods.config.yml.datasource.DataSourceProperties;
import org.open4goods.crawler.services.fetching.AggregatedDataBackedFetchingService;
import org.open4goods.model.crawlers.FetchingJobStats;
import org.open4goods.model.data.DataFragment;
import org.open4goods.model.product.AggregatedData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract stuff for datasources that behaves from existing AggregatedDatas (thinked for apis)
 * @author goulven
 *
 */
public abstract class AbstractAggregatedDataWorker implements Runnable{

	// Default logger, that is overriden by genericFileLogger. But for tests, it is not always filled
	protected  Logger dedicatedLogger = LoggerFactory.getLogger(AbstractAggregatedDataWorker.class);

	protected DataSourceProperties provider;

	private String datasourceConfName;

	private final AtomicBoolean running = new AtomicBoolean(false);

	private AggregatedDataBackedFetchingService fetchingService;

	private static final Pattern lineSeparator = Pattern.compile(System.lineSeparator());

	private FetchingJobStats stat;

	/** "Post init", due to the dynamic instanciation. @SeeAggregatedDataBackedFetchingService
	 * @param aggregatedDataBackedFetchingService
	 * @param stat
	 * @param dedicatedLogger **/
	public void init(final DataSourceProperties provider, final String datasourceConfName,
			final AggregatedDataBackedFetchingService aggregatedDataBackedFetchingService, final FetchingJobStats stat, final Logger dedicatedLogger) {
		this.provider = provider;
		this.datasourceConfName = datasourceConfName;

		fetchingService = aggregatedDataBackedFetchingService;
		this.stat = stat;
		this.dedicatedLogger=dedicatedLogger;

	}


	/**
	 * TODO(design, P3, 0.5) : should be a dedicated, tested service
	 *
	 * @return
	 */
	private Stream<AggregatedData> getAggregatedDatas() {

		try {
			//TODO(feature,1,P1) : handle when multiple verticals
			//TODO(0.25,perf,p3) : use http client
			//TODO(conf,0.25,P3) : from conf
			final Stream<AggregatedData> ret = lineSeparator.splitAsStream(IOUtils.toString(new URL("http://localhost:8082/api/export"))).map( e -> {
				try {
					return fetchingService.getSerialisationService().fromJson(e, AggregatedData.class);
				} catch (final Exception e1) {
					dedicatedLogger.error("Error while deserializing data",e1);
					return null;
				}
			}) ;

			return ret;

		} catch (final Exception e) {
			dedicatedLogger.error("Error while streaming datas from capsule",e);
		}
		return Stream.empty();

	}



	@Override
	public void run() {

		running.set(true);
		// For each aggregated data to build
		//TODO(gof) parametize url here
		getAggregatedDatas().forEach(data -> {

			dedicatedLogger.info("{} is fetching data for {}", getClass().getName(), data);
			try {
				// If the process is running
				if (running.get()) {

					final Set<DataFragment> fragments = fragmentsFor(data,provider);

					if (null == fragments) {
						dedicatedLogger.warn("A null fragment returned for {} in {}",data,getClass().getName());
						return;
					}
					stat.incrementProcessed(fragments.size());

					dedicatedLogger.info("{} datafragment retrieved on {} for {}",fragments.size(), provider, data);
					for (final DataFragment df : fragments) {

						//TODO(design) : see generic logger here
						fetchingService.getCompletionService().complete(df, datasourceConfName, provider, dedicatedLogger);

						// Indexation
						fetchingService.getIndexationService(). index(df, datasourceConfName);
					}
				}
			} catch (final Exception e) {
				dedicatedLogger.error("Error while fetching api backed data for {}",data,e);
			}

			// Making the pause
			try {
				Thread.sleep(provider.getApiDatasource(). getApiDelaySeconds()*1000);
			} catch (final Exception e) {
				dedicatedLogger.error("Error while sleeping thread,e");

			}
		});

		running.set(false);
		fetchingService.getRunning().remove(datasourceConfName);

	}

	public void stop() {
		running.set(false);
	}


	public abstract Set<DataFragment> fragmentsFor(AggregatedData data, DataSourceProperties provider) ;





}
