package org.open4goods.services;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.open4goods.config.BrandsConfiguration;
import org.open4goods.config.yml.datasource.DataSourceProperties;
import org.open4goods.exceptions.InvalidParameterException;
import org.open4goods.model.data.Brand;
import org.open4goods.model.data.BrandScore;
import org.open4goods.store.repository.elastic.BrandScoresRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.googlecode.concurrenttrees.radix.ConcurrentRadixTree;
import com.googlecode.concurrenttrees.radix.RadixTree;
import com.googlecode.concurrenttrees.radix.node.concrete.DefaultCharArrayNodeFactory;

/**
 * This service handles XWiki auth and xwiki content retrieving TODO : Should
 * have strong images (instead of dependant on various datasources) TODO :
 * Should have logo converted to wlp / png, with meta TODO : Having a service
 * that manages image size reduction TODO : Better load / cache at startup
 * 
 * @author Goulven.Furet
 */
public class BrandService {

	private static final Logger logger = LoggerFactory.getLogger(BrandService.class);

	private RemoteFileCachingService remoteFileCachingService;

	private BrandsConfiguration brandsConfig;
	private BrandScoresRepository brandRepository;

	private Map<String, Brand> brandsByName = new HashMap<>();
	private Map<String, Brand> brandsByAka = new HashMap<>();
	
    RadixTree<Brand> brandsByRadix = new ConcurrentRadixTree<Brand>(new DefaultCharArrayNodeFactory());


	public BrandService(BrandsConfiguration config, RemoteFileCachingService remoteFileCachingService, BrandScoresRepository brandRepository) {
		this.brandsConfig = config;
		this.remoteFileCachingService = remoteFileCachingService;
		this.brandRepository = brandRepository;

		// Updating maps
		for (Brand b : brandsConfig.getBrands()) {
			saveBrand(b);
		}
	}

	/**
	 * Get a brand by it's name
	 * 
	 * @param name
	 * @return
	 */
	public Brand getBrandByName(String name) {
		if (StringUtils.isEmpty(name)) {
			return null;
		}
		return brandsByName.get(name.toUpperCase());
	}

	/**
	 * Resolve a brand name, by trying to find it in the list of known brands
	 * 
	 * @param name
	 * @return
	 */
	public Brand resolveBrandName(String name) {
		String input = getKeyName(name);

		logger.info("Resolving brand {} ({})", name, input);
		
		if (brandsConfig.getBrandsToRemove().contains(input)) {
			return null;
		}

		// Direct resolution
		Brand ret = getBrandByName(name);

		
		// Trying by removing commons suffixes
		if (null == ret) {
			String cleaned = StringUtils.removeEnd(name, " CORP.");
			cleaned = StringUtils.removeEnd(cleaned, " INC.");
			ret = getBrandByName(cleaned);			
		}
		
		
		if (null == ret) {
			// Resolution by aka names
			ret = brandsByAka.get(name);
		}
		
		// TODO : Gof perf, perf, perf !! But don't really know how to perform better
		
		if (null == ret) {
			Set<Brand> matches = new HashSet<>();
			for (Entry<String, Brand> entry : brandsByName.entrySet()) {
			
				if (input.startsWith(entry.getKey())) {
					matches.add(entry.getValue());
				}
			}

			if (matches.size() == 1) {
				ret = matches.iterator().next();
				logger.warn("Found a prefix match for brand {} with {}. Please consider adding it in the aka names", name, ret);				
				return matches.iterator().next();
			} else if (matches.size() > 1) {
				logger.warn("Found multiple prefix matches for brand {}. Please consider adding one of them in config {}", name, matches);
			}
		}
		
		return ret;
	}

	/**
	 * Return true if a logo exists for this brand
	 * 
	 * @param brand
	 * @return
	 */
	public boolean hasLogo(String brand) {
		Brand b = resolveBrandName(brand);
		if (null == b) {
			return false;
		} else {
			return StringUtils.isEmpty(b.getLogo());
		}
	}

	/**
	 * Get the logo inputstream
	 * 
	 * @param brand
	 * @return
	 * @throws IOException
	 * @throws InvalidParameterException
	 * @throws FileNotFoundException
	 */
	public InputStream getLogo(String brand) throws InvalidParameterException, FileNotFoundException, IOException {

		Brand b = resolveBrandName(brand);

		if (b == null || StringUtils.isEmpty(b.getLogo())) {
			return null;
		}

		File f = remoteFileCachingService.getResource(b.getLogo());
		if (f == null) {
			throw new FileNotFoundException("File not found for logo: " + b.getLogo());
		}

		try (FileInputStream fis = new FileInputStream(f); BufferedInputStream bis = new BufferedInputStream(fis)) {
			return IOUtils.toBufferedInputStream(bis);
		}
	}

	/**
	 * Create or update a score for a given brand
	 * 
	 * @param brand
	 * @param datasourceProperties
	 * @param scoreValue
	 */
	public void addBrandScore(String brand, DataSourceProperties datasourceProperties, String scoreValue) {

		Brand normalizedBrand = resolveBrandName(brand);

		if (null == normalizedBrand) {
			logger.info("Brand {} is not valid, skipping", brand);
			return;
		}

		logger.info("Adding brand score {}:{} for brand {} ({}) ", datasourceProperties.getName(), scoreValue, normalizedBrand, brand);

		BrandScore b = brandRepository.findById(normalizedBrand.getName()).orElse(new BrandScore(normalizedBrand.getName()));
		b.setLastUpdate(System.currentTimeMillis());

		try {
			if (null != datasourceProperties.getInvertScaleBase()) {
				b.getScores().put(datasourceProperties.getName(),
						(datasourceProperties.getInvertScaleBase() - Double.valueOf(scoreValue)));
			} else {
				b.getScores().put(datasourceProperties.getName(), Double.valueOf(scoreValue));
			}
			logger.info("Saving brand {}", b);
			brandRepository.save(b);
		} catch (NumberFormatException e) {
			logger.error("Cannot parse score value {} for brand {}", scoreValue, brand);
		}
	}
	
	/**
	 * Return the hashed key name for a brand
	 * 
	 * @param name
	 * @return
	 */
	private String getKeyName(String name) {
		return StringUtils.stripAccents(name.toUpperCase()).trim();
	}

	/**
	 * Save a brand
	 * 
	 * @param brand
	 */
	public void saveBrand(Brand brand) {
		String bName = getKeyName(brand.getName());
		
		brandsByName.put(bName, brand);
		brandsByRadix.put(bName, brand);
		
		for (String aka : brand.getAka()) {
			String akaName = getKeyName(aka);
			brandsByAka.put(akaName, brand);
			brandsByRadix.put(bName, brand);
		}		
	}

}