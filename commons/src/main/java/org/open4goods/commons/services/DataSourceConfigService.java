package org.open4goods.commons.services;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.open4goods.commons.config.yml.datasource.DataSourceProperties;
import org.open4goods.commons.config.yml.datasource.HtmlDataSourceProperties;
import org.open4goods.model.constants.CacheConstants;
import org.open4goods.model.exceptions.InvalidParameterException;
import org.open4goods.services.remotefilecaching.service.RemoteFileCachingService;
import org.open4goods.services.serialisation.service.SerialisationService;
import org.quartz.CronExpression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

/**
 * This service is in charge to provide the DataSource configurations. and informations about them. For now stored in the app config,
 * @author goulven
 *
 * TODO : this class is a mess (caching datasource must be done at class init)
 *
 */
public class DataSourceConfigService {

	protected static final Logger logger = LoggerFactory.getLogger(DataSourceConfigService.class);

	//TODO(gof) : inject in constructor
	private @Autowired SerialisationService serialisationService;

	private @Autowired RemoteFileCachingService cachingService;


	/**
	 * The folders where are stored datasourcesByFileName
	 */
	private final String datasourceConfigFolder;

	// Used to load Datasource configurations from classpath
	private static final ClassLoader cl = DataSourceConfigService.class.getClassLoader();
	private static final ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(cl);

	private final Map<String,DataSourceProperties> additionalDatasources = new HashMap<>();

	// Self maintened cache (@Cacheable problem when internaly called)
	private Map<String,DataSourceProperties> datasourcesByFileName = null;
	private Map<String,DataSourceProperties> datasourcesByConfigName = new HashMap<>();
	private Map<String,DataSourceProperties> datasourcesByFeedKey = new HashMap<>();
	
	
	public DataSourceConfigService(final String datasourceConfigFolder) {
		super();
		this.datasourceConfigFolder = datasourceConfigFolder;
	}



	/**
	 * Return a cached stream to the datasource favico
	 * @param datasourceName
	 * @return
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws InvalidParameterException
	 */
	public InputStream getFavicon(String datasourceName) throws FileNotFoundException, IOException, InvalidParameterException {

		// Initialising cache if necessary
		datasourceConfigs();

		DataSourceProperties dataSourceProperties = datasourcesByConfigName.get(datasourceName);
		
		if (null == dataSourceProperties) {
			throw new InvalidParameterException("Datasource " + datasourceName + " not found");
		}
		
		// TODO(p3, conf) : freshness from conf
		try {File f = cachingService.getResource(dataSourceProperties.getFavico(), 1 );
			return IOUtils.toBufferedInputStream(new FileInputStream(f));
		} catch (Exception e) {
			logger.error("Error while loading favicon for datasource {} : {} --> {}",  datasourceName, dataSourceProperties.getFavico(), e.getMessage() );
			// TODO : Return empty image bytes
			// TODO : Log for action
			return null;
		}
	}


	public InputStream getLogo(String datasourceName) throws FileNotFoundException, IOException, InvalidParameterException {

		// Initialising cache if necessary
		datasourceConfigs();

		// TODO(p3, conf) : freshness from conf
		File f = cachingService.getResource(datasourcesByConfigName.get(datasourceName).getLogo(),1 );
		return IOUtils.toBufferedInputStream(new FileInputStream(f));
	}

	
	/**
	 * Return the next Date the given datasource will be fetched
	 * @param datasourceName
	 * @return
	 */
	public Date getNextSchedule(final String datasourceName) {
		final DataSourceProperties p = getDatasourceConfig(datasourceName);
		if (null != p) {
			return getNextDateForCron(p.cron());
		}
		return null;
	}


	/**
	 * Return a specific Datasource
	 * @param datasourceName
	 * @return
	 */
	@Cacheable(keyGenerator = CacheConstants.KEY_GENERATOR, cacheNames=CacheConstants.FOREVER_LOCAL_CACHE_NAME)
	public DataSourceProperties getDatasourceConfig(final String datasourceName) {
		return datasourceConfigs().get(datasourceName);

	}


	//NOTE(gof) : cache not working on inner class calls. SelfMade cache for safety
	//	@Cacheable(cacheNames=CacheConstants.FOREVER_LOCAL_CACHE_NAME)
	public Map<String,DataSourceProperties> datasourceConfigs() {

		if (null != datasourcesByFileName) {
			return datasourcesByFileName;
		}

		datasourcesByFileName = new HashMap<>();
		datasourcesByFileName.putAll(additionalDatasources);

		datasourcesByFileName.putAll(getDatasourceConfigs("file:"+datasourceConfigFolder+File.separator+"**", resolver));

		// Fill the by config name

		for (DataSourceProperties conf : datasourcesByFileName.values()) {
			datasourcesByConfigName.put(conf.getName(), conf);
			datasourcesByFeedKey.put(conf.getFeedKey(), conf);
		}



		return datasourcesByFileName;
	}


	/**
	 * Return the DatasourceProperties matching the url given in parameter. Resolution is done on baseUrl, that must be conta
	 * @return
	 */
	public DataSourceProperties getDatasourcePropertiesForUrl(final String url ) {

		for (final DataSourceProperties dsp : datasourceConfigs().values()) {
			final HtmlDataSourceProperties webdatasource = dsp.webDataSource();
			if (null != webdatasource) {
				if (StringUtils.isEmpty(webdatasource.getBaseUrl())  ) {
					logger.error("Base url is undefined for datasource {}",dsp);
				}
				else {
					if (url.contains(webdatasource.getBaseUrl())) {
						return dsp;
					}
				}
			}
		}
		return null;
	}

	/**
	 * Return a date of next execution for a given cron exp
	 *
	 * @param cron
	 * @return
	 */
	public Date getNextDateForCron(final String cron) {
		try {
			final CronExpression cronExpression = new CronExpression(cron);
			cronExpression.getNextValidTimeAfter(new Date());
		} catch (final Exception e) {
			logger.warn("Error while generating date from cron ", e);
		}
		return null;
	}


	/**
	 * Instanciate the datasourcesByFileName config from a given classpath
	 *
	 * @param path
	 * @param resolver
	 * @return
	 * @throws IOException
	 */
	private  Map<String,DataSourceProperties> getDatasourceConfigs(final String path, final ResourcePatternResolver resolver) {
		final Map<String,DataSourceProperties> ret = new HashMap<>();
		org.springframework.core.io.Resource[] resources = null;

		logger.info("Reading datasources ByFileName from: {}", path);

		try {
			resources = resolver.getResources(path);

			for (final org.springframework.core.io.Resource resource : resources) {

				try {
					logger.info("found provider config : {}", resource.getFilename());
					final DataSourceProperties provider = serialisationService.fromYaml(resource.getInputStream(), DataSourceProperties.class);
					provider.setDatasourceConfigName(resource.getFilename());
					ret.put(resource.getFilename(),provider);
				} catch (final Exception e) {
					try {
						logger.error("Error while loading provider named '{}' with URI '{}': {}",
								resource.getFilename(), resource.getURI().toASCIIString(), e.getMessage());

					} catch (final IOException e1) {
						logger.error("Unexpected error", e1);
					}
				}
			}
		} catch (final Exception e) {
			logger.error("Error while loading datasourcesByFileName definitions from {}", path, e);
		}

		return ret;
	}


	/**
	 * Return a datasource for a given feedKey
	 * @param feedKey
	 * @return
	 */
	public DataSourceProperties getDatasourcePropertiesForFeed(String feedKey) {
		return datasourcesByFeedKey.get(feedKey);
	}



	public DataSourceProperties getDefaultDataSource() {
		return new DataSourceProperties();
	}

}
