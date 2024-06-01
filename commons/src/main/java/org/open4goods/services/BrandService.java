package org.open4goods.services;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.open4goods.config.BrandConfiguration;
import org.open4goods.config.yml.datasource.DataSourceProperties;
import org.open4goods.exceptions.InvalidParameterException;
import org.open4goods.model.data.Brand;
import org.open4goods.store.repository.elastic.BrandRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This service handles XWiki auth and xwiki content retrieving
 * TODO : Should have strong images (instead of dependant on various datasources)
 * TODO : Should have logo converted to wlp / png, with meta
 * TODO : Having a service that manages  image size reduction
 * TODO : Better load / cache at startup
 * @author Goulven.Furet
 */
public class BrandService {

	private static final Logger logger = LoggerFactory.getLogger(BrandService.class);

	private RemoteFileCachingService remoteFileCachingService;

	private BrandConfiguration brandsConfig;

	private BrandRepository brandRepository;

	public BrandService(BrandConfiguration config, RemoteFileCachingService remoteFileCachingService, BrandRepository brandRepository) {
		this.brandsConfig = config;
		this.remoteFileCachingService = remoteFileCachingService;
		this.brandRepository = brandRepository;
	}

	
	/**
	 * Return the brand from a given one
	 */
	
	public String normalizeBrand (String brand) {
		
		String input = StringUtils.stripAccents(brand.toUpperCase()).trim();
		
		if (brandsConfig.getBrandsToRemove().contains(input)) {
			return null;
		}
				
		String target = brandsConfig.getBrandsToReplace().get(input);
		
		if (null != target) {
			return target;
		} 
		
		return input;		
	}
	
	
	/**
	 * Return the logo imagename, as a "samsung.png" 
	 * @param brand
	 * @return
	 */
	public String getLogoImageName(String brand) {
		
		String ret = null;		
		if (hasLogo(brand)) {
			// TODO : operate a real image conversion
			return brand+".png";
		}
		
		return ret;
	}
	
	
	/**
	 * Return true if a logo exists for this brand
	 * @param brand
	 * @return
	 */
	public boolean hasLogo(String brand) {
		return brandsConfig.getBrandsLogo().containsKey(brand);
	}
	
	/**
	 * Get the logo inputstream
	 * @param brand
	 * @return
	 * @throws IOException
	 * @throws InvalidParameterException 
	 * @throws FileNotFoundException 
	 */
	public InputStream getLogo(String brand) throws  InvalidParameterException, FileNotFoundException, IOException{
		
		if (null == brand || !brandsConfig.getBrandsLogo().containsKey(brand)) {
			throw new InvalidParameterException("No data fror brand " + brand);
		}
		
		String url = brandsConfig.getBrandsLogo().get(brand);
		if (null == url) {
			throw new FileNotFoundException("No source image for brand " + brand);
		}
		
		File f = remoteFileCachingService.getResource(url);
//		remoteFileCachingService.
		return IOUtils.toBufferedInputStream(new FileInputStream(f));
	}

	
	/**
	 * Create or update a score for a given brand
	 * @param brand
	 * @param datasourceProperties
	 * @param scoreValue
	 */
	public void addBrandScore(String brand, DataSourceProperties datasourceProperties, String scoreValue) {
		
		String normalizedBrand = normalizeBrand(brand);
		
		if (StringUtils.isEmpty(normalizedBrand)) {
			logger.info("Brand {} is not valid, skipping", brand);
            return;
        }
		
		logger.info("Adding brand score {}:{} for brand {} ({}) ",datasourceProperties.getName(), scoreValue, normalizedBrand, brand);
		
		Brand b = brandRepository.findById(normalizedBrand).orElse(new Brand(normalizedBrand));		
		b.setLastUpdate(System.currentTimeMillis());

		try {
			if (null != datasourceProperties.getInvertScaleBase()) {
				b.getScores().put(datasourceProperties.getName(),  (datasourceProperties.getInvertScaleBase() - Double.valueOf(scoreValue)));			
			} else {			
				b.getScores().put(datasourceProperties.getName(), Double.valueOf(scoreValue));
			}
			logger.info("Saving brand {}", b);
			brandRepository.save(b);
		} catch (NumberFormatException e) {
			logger.error("Cannot parse score value {} for brand {}", scoreValue, brand);
		}
		
		
		
		
		
		
		
	}
	


}