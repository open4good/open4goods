package org.open4goods.services;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.open4goods.config.yml.IcecatFeatureConfiguration;
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
		
	
	
	public IcecatFeatureService(XmlMapper xmlMapper, IcecatFeatureConfiguration iceCatConfig) {
		super();
		this.xmlMapper = xmlMapper;
		this.iceCatConfig = iceCatConfig;
	}



	/**
	 * Load features from the IceCat XML file.
	 */
//	@PostConstruct
	public void loadFeatures() {
		
		// TODO : Handle URL
		LOGGER.info("Loading features from : {}", iceCatConfig.getFeaturesListFileUri());
		File f = new File(iceCatConfig.getFeaturesListFileUri());
		 try {
			 List<IcecatFeature> features = xmlMapper.readValue(f, IcecatModel.class).getResponse().getFeaturesList().getFeatures();
			
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
                    	LOGGER.warn("Feature name {} map's multiple features ({}) ", name.getValue(), fIds);
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
	
	
}
