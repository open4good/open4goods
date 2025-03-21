package org.open4goods.commons.services;

import java.io.InputStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.open4goods.commons.helper.GenericFileLogger;
import org.open4goods.commons.model.data.Brand;
import org.open4goods.services.remotefilecaching.service.RemoteFileCachingService;
import org.open4goods.services.serialisation.service.SerialisationService;
import org.slf4j.Logger;

import com.fasterxml.jackson.core.type.TypeReference;

import ch.qos.logback.classic.Level;

/**
 * This service handles brands resolution, and associated companies matching.
 * It is based on the open mapping defined here : https://github.com/open4good/brands-company-mapping
 * 
 * @author Goulven.Furet
 */
public class BrandService {

	private  final Logger logger ;

	private RemoteFileCachingService remoteFileCachingService;
	// The map that maintains stats on brands for the unresolvable companies
	private Map<String,Long> missCounter = new ConcurrentHashMap<String, Long>();
	

	private Map<String, Brand> brandsByName = new HashMap<>();
	
	public BrandService( RemoteFileCachingService remoteFileCachingService, String logsFolder, SerialisationService serialisatonService) throws Exception {
		this.remoteFileCachingService = remoteFileCachingService;
		this.logger = 	GenericFileLogger.initLogger("brand-service", Level.WARN, logsFolder);
		
		// TODO : Load brands from github
		
		try {
			//TODO(p2, conf) : from conf
			String mappingUrl = "https://raw.githubusercontent.com/open4good/brands-company-mapping/refs/heads/main/brands-company-mapping.json";
			String mappingsStr = IOUtils.toString(new URL(mappingUrl ), Charset.defaultCharset());
			
			Map<String,String> mappings = serialisatonService.fromJson(mappingsStr,    new TypeReference<HashMap<String, String>>() {});
			
			mappings.entrySet().forEach(keyVal -> {
				Brand b = new Brand(keyVal.getKey());
				b.setCompanyName(keyVal.getValue());
				brandsByName.put(keyVal.getKey(), b);
			});
		} catch (Exception e) {
			logger.error("Error while loading categories",e);
			// This is a major issue, avoid the container start
			throw e;
		}
		
	}

	/**
	 * Resolve a brand name, by trying to find it in the list of companies associated brands
	 * Will return a Brand with unassociated company other else 
	 * @param brandName
	 * @return
	 */
	// TODO : cache
	public Brand resolve(String brandName) {
		String input = sanitizeBrand(brandName);
		logger.info("Resolving brand {} ({})", brandName, input);

		Brand ret = brandsByName.get(input);
		
		if (null == ret) {
			ret = new Brand(input);
			logger.info("Brand not found in companies mapping : {}",ret);

		} else {
			logger.info("Brand found in companies mapping : {}",ret);
		}
		
		return ret;
	}



	/**
	 * Return the hashed key name for a brand. 
	 * Items defined in the mapping file MUST apply this sanitization
	 * 
	 * @param name
	 * @return
	 */
	public String sanitizeBrand(String name) {
		if (StringUtils.isEmpty(name)) {
			return "";
		}
		return StringUtils.stripAccents(name.toUpperCase()).trim();
	}

	/**
	 * Helper, to stat brand that do not have mappings
	 * @param brand
	 */
	public void incrementUnknown(String brand) {
		Long counter = missCounter.get(brand);
		if (null == counter) {
			counter = 1L;
		} else {
			counter ++;
		}
		missCounter.put(brand, counter);
	}

	public boolean hasLogo(String upperCase) {
		// TODO Auto-generated method stub
		return false;
	}

	public InputStream getLogo(String upperCase) {
		// TODO Auto-generated method stub
		return null;
	}

	public Map<String, Long> getMissCounter() {
		return missCounter;
	}

	


}