package org.open4goods.services;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.open4goods.config.yml.IcecatFeatureConfiguration;
import org.open4goods.exceptions.TechnicalException;
import org.open4goods.helper.IdHelper;
import org.open4goods.model.icecat.IcecatFeature;
import org.open4goods.model.icecat.IcecatModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;

import jakarta.annotation.PostConstruct;


/**
 * This service maps expose Icat features.
 * 
 */
public class IcecatFeatureService {

		private  Logger LOGGER = LoggerFactory.getLogger(IcecatFeatureService.class);
		private XmlMapper xmlMapper;
		private IcecatFeatureConfiguration iceCatConfig;

		private Map<Long, IcecatFeature> featuresById = new HashMap<>();
		private Map<String, Set<Long>> featuresByNames = new HashMap<>();
		
		private RemoteFileCachingService fileCachingService;
		private String remoteCachingFolder;
	
	
	public IcecatFeatureService(XmlMapper xmlMapper, IcecatFeatureConfiguration iceCatConfig, RemoteFileCachingService fileCachingService, String remoteCacheFolder) {
		super();
		this.xmlMapper = xmlMapper;
		this.iceCatConfig = iceCatConfig;
		this.fileCachingService = fileCachingService;
		this.remoteCachingFolder = remoteCacheFolder;
	}

	/**
	 * Load features from the IceCat XML file.
	 * @throws TechnicalException 
	 * TODO : Should be done in a separate thread 
	 */
	@PostConstruct
	public void loadFeatures() throws TechnicalException {
		
		// 1 - Download the file with basic auth
		
		// Unzip it
		if (null == iceCatConfig.getFeaturesListFileUri()) {
			LOGGER.error("No features list file uri configured");
			return;
		}
		File icecatFile = getCachedFile(iceCatConfig.getFeaturesListFileUri(), iceCatConfig.getUser(), iceCatConfig.getPassword());
		

		 try {
			 List<IcecatFeature> features = xmlMapper.readValue(icecatFile, IcecatModel.class).getResponse().getFeaturesList().getFeatures();
			
			features.forEach(feature -> {
				
				Long id = Long.valueOf(feature.getID());
				// Loading the by id map
				featuresById.put(id, feature);
				
				// Loading the by name map
				feature.getNames().getNames().forEach(name -> {
                  
					String val = normalize(name.getValue());
					Set<Long> fIds = featuresByNames.get(val);
                    if (fIds == null) {
                        fIds = new HashSet<>();
                    }
                    fIds.add(id);
                    featuresByNames.put(val, fIds);

                    if (fIds.size() > 1) {
                    	LOGGER.info("Feature name {} map's multiple features ({}) ", name.getValue(), fIds);
                    }

                });
			});
			
			
		} catch (Exception e) {
			LOGGER.error("Error while loading features", e);
		}
		 LOGGER.info("End loading of features from {}", iceCatConfig.getFeaturesListFileUri());
	}
	

	/**
	 * Resolve a feature name to one or more feature ID.
	 * @param featureName
	 * @return
	 */
	public Set<Long> resolve (String featureName) {		
		String f = normalize(featureName);
		return featuresByNames.get(f);		
	}



	private String normalize(String featureName) {
		return StringUtils.normalizeSpace(StringUtils.stripAccents(featureName)).toLowerCase();
	}
	
	
	

	/**
	 * Donload feature file, unzip it and maintain the cached version
	 * TODO : Should defer the "cahce" url <> file in the remotefilecachingservice
	 * @param url
	 * @param user
	 * @param password
	 * @return
	 * @throws TechnicalException
	 */
	private File getCachedFile(String url, String user, String password) throws TechnicalException {

		LOGGER.info("Retrieving file : {}", url);
		File destFile = new File(remoteCachingFolder+File.separator+IdHelper.getHashedName(url));		
		// Return the cached file if it exists
		if (destFile.exists()) {
			// TODO : Have a refresh policy
			LOGGER.info("File {} already cached", url);
			return destFile;
		}
		
		File tmpFile = new File(remoteCachingFolder+File.separator+"tmp-"+IdHelper.getHashedName(url));		
		try {
			LOGGER.info("Starting download : {}", url);
			fileCachingService.downloadTo(user, password, url, tmpFile);
			LOGGER.info("Uncompressing file : {}", tmpFile);
			fileCachingService.decompressGzipFile(tmpFile,destFile);
			LOGGER.info("File {} uncompressed", url);
			return destFile;
		} catch (Exception e) {
			throw new TechnicalException("Error retrieving resource",e);
		} finally {
			FileUtils.deleteQuietly(tmpFile);			
		}
	}
	
}
