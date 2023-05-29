package org.open4goods.services;
import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.io.FileUtils;
import org.open4goods.exceptions.InvalidParameterException;
import org.open4goods.exceptions.TechnicalException;
import org.open4goods.helper.IdHelper;
import org.open4goods.model.CacheResourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;


/**
 * This service is in charge of periodicaly download and make availlable remote files. It also allows to unzip / untar
 * @author goulven
 *

 * TODO(0.25,P3,test) : unit tests
 */
public class RemoteFileCachingService {


	private final Map<String,CacheResourceConfig> configs = new HashMap<>();

	private static final Logger logger = LoggerFactory.getLogger(RemoteFileCachingService.class);

	private final String resourceFolder;


	public RemoteFileCachingService(final String resourceFolder) {
		super();
		this.resourceFolder = resourceFolder;
		final File rFolder = new File(resourceFolder);
		rFolder.mkdirs();
	}


	public void cacheResource(final CacheResourceConfig resourceConfig) {
		configs.put(resourceConfig.getUrl(), resourceConfig);
	}


	public File getResource(final String url) throws InvalidParameterException {

		File resource = new File(resourceFolder+"/"+IdHelper.getHashedName(url));

		CacheResourceConfig conf = configs.get(url);
		if (null == conf) {
			logger.info("No cache config, caching with default config",url );
			conf = new CacheResourceConfig();
			conf.setUrl(url);
			configs.put(url, conf);
		}

		try {
			if (!resource.exists()) {
				logger.info("Resource {} does not exists, will download it",url);
				resource = retrieve(conf);
			} else if ((System.currentTimeMillis() - resource.lastModified()) > (conf.getRefreshInDays() * 24 *3600 * 1000)) {
				logger.warn("Resource {} is outdated, will replace it",url);
				resource = retrieve(conf);
			}
		} catch (final Exception e) {
			logger.error("Resource {} cannot be retrieved",url,e );
		}

		return resource;

	}



	private File retrieve(final CacheResourceConfig conf) throws TechnicalException {

		try {
			File tmpFile = new File(resourceFolder+"/tmp-"+IdHelper.getHashedName(conf.getUrl()));
			File destFile = new File(resourceFolder+"/"+IdHelper.getHashedName(conf.getUrl()));

			tmpFile = download(conf.getUrl(), tmpFile);



			if (conf.getUnzip()) {
				destFile = unzip(tmpFile,destFile,conf);
			} else {
				tmpFile.renameTo(destFile);
			}


			return destFile;
		} catch (TechnicalException e) {
			throw e;
		} catch (Exception e) {
			throw new TechnicalException("Error retrieving resource",e);
		}
	}


	private File unzip(final File tmpFile, final File destFile, final CacheResourceConfig conf) throws ZipException {
		final ZipFile zipFile = new ZipFile(tmpFile);
		//		File zipedDestFile = File.createTempFile("csv_zipped", dsProperties.getName());

		final String targetFolder = tmpFile.getParent() + "/" + "unziped";

		logger.info("Unzipping CSV data from {} to {}", tmpFile.getAbsolutePath(), targetFolder);

		new File(targetFolder).mkdirs();
		zipFile.extractAll(targetFolder);

		final File zipedDestFolder = new File(targetFolder);
		File res = null;


		if (zipedDestFolder.list().length > 1) {

			for (final File child : zipedDestFolder.listFiles()) {
				if (child.getName().equals(conf.getExtractedFileName())) {
					res = child;
					break;
				}
			}
		} else {
			res = zipedDestFolder.listFiles()[0];
		}

		if (null == res) {
			logger.warn("No result files in zip extraction, of ultiple and no extractedfilename in config");
		} else {
			res.renameTo(destFile);
		}

		IOUtils.closeQuietly(zipFile);
		destFile.setLastModified(System.currentTimeMillis());
		return destFile;

	}


	public File download(final String url,final File tmpFile) throws TechnicalException {
		// local file download, then estimate number of rows

		try {
			logger.info("Downloading resource  from {} to {}", url, tmpFile);
			FileUtils.copyURLToFile(new URL(url), tmpFile);
			return tmpFile;
		} catch (Exception e) {
			throw new TechnicalException("Cannot download resource " + url  + " : " + e.getMessage());
		}
	}

}