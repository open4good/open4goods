package org.open4goods.commons.services;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.open4goods.commons.config.BrandsConfiguration;
import org.open4goods.commons.config.yml.datasource.DataSourceProperties;
import org.open4goods.commons.exceptions.InvalidParameterException;
import org.open4goods.commons.helper.GenericFileLogger;
import org.open4goods.commons.model.constants.CacheConstants;
import org.open4goods.commons.model.data.Brand;
import org.open4goods.commons.model.data.BrandScore;
import org.open4goods.commons.store.repository.elastic.BrandScoresRepository;
import org.slf4j.Logger;
import org.springframework.cache.annotation.Cacheable;

import com.googlecode.concurrenttrees.radix.ConcurrentRadixTree;
import com.googlecode.concurrenttrees.radix.RadixTree;
import com.googlecode.concurrenttrees.radix.node.concrete.DefaultCharArrayNodeFactory;

import ch.qos.logback.classic.Level;

/**
 * This service handles XWiki auth and xwiki content retrieving TODO : Should
 * have strong images (instead of dependant on various datasources) TODO :
 * Should have logo converted to wlp / png, with meta TODO : Having a service
 * that manages image size reduction TODO : Better load / cache at startup
 * 
 * @author Goulven.Furet
 */
public class BrandService {

	private  final Logger logger ;

	private RemoteFileCachingService remoteFileCachingService;

	private BrandsConfiguration brandsConfig;
	private BrandScoresRepository brandRepository;

	private Map<String, Brand> brandsByName = new HashMap<>();
	private Map<String, Brand> brandsByAka = new HashMap<>();
	
    RadixTree<Brand> brandsByRadix = new ConcurrentRadixTree<Brand>(new DefaultCharArrayNodeFactory());


	public BrandService(BrandsConfiguration config, RemoteFileCachingService remoteFileCachingService, BrandScoresRepository brandRepository, String logsFolder) {
		this.brandsConfig = config;
		this.remoteFileCachingService = remoteFileCachingService;
		this.brandRepository = brandRepository;
		this.logger = 	GenericFileLogger.initLogger("brand-service", Level.INFO, logsFolder);
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
	 *  TODO : A lot of call.. See impact of radix / and have a fast cache
	 * @param name
	 * @return
	 */
	public Brand resolveCompanyFromBrandName(String name) {
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
		Brand b = resolveCompanyFromBrandName(brand);
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

		Brand b = resolveCompanyFromBrandName(brand);

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

		if (StringUtils.isEmpty(brand) || StringUtils.isEmpty(scoreValue)) {
			logger.info("Cannot proceed empty brand or score, skipping");
			return;
		}
		
//		Brand rb = resolveBrandName(brand);
//		if (null == rb) {
//			logger.warn("Unknown brand {}. Brand score will be added", brand);
//			return;
//		}
//		

		logger.info("Adding brand score {}:{} for brand {}", datasourceProperties.getName(), scoreValue, brand);

		BrandScore brandScore = new BrandScore(datasourceProperties, brand, scoreValue);
		

		
		
//		String id = brandScore.getId();
		
//		BrandScore b = brandRepository.findById(id).orElse(newBrand);
//		b.setLastUpdate(System.currentTimeMillis());

		logger.info("Saving brand {}", brandScore);
		brandRepository.save(brandScore);

	}
	
	/**
	 * Return the hashed key name for a brand
	 * 
	 * @param name
	 * @return
	 */
	private String getKeyName(String name) {
		if (StringUtils.isEmpty(name)) {
			return "";
		}
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

	/**
	 * Operates a brand name resolution against the brandscore repository, 
	 * if successfull retrieve the associated score
	 * @param brand
	 * @param string
	 * @return
	 */
	@Cacheable(keyGenerator = CacheConstants.KEY_GENERATOR, cacheNames = CacheConstants.ONE_HOUR_LOCAL_CACHE_NAME)
	public Double getBrandScore(String brand, String datasourceName) {
		Double score = null;
		
		brand = brand.trim().toLowerCase();
		
		// 1 : Trying a direct resolution
		// TODO : With a find byid
		
		List<BrandScore> results = new ArrayList<>();
		String id = BrandScore.id(datasourceName, brand);
		
		BrandScore result = brandRepository.findById(id).orElse(null);		
		if (null != result) {
			logger.info("Score found with a direct resolution for brand {} and datasource {}", brand, datasourceName);
			score = Double.valueOf(result.getNormalized());
			return score;
		}
		
		
		String bName =  URLEncoder.encode(brand);
		if (null == score) {
			// Trying a prefix resolution
			logger.info("Trying a prefix resolution for brand {} and datasource {}", brand, datasourceName);
			results = brandRepository.findByDatasourceNameAndBrandNameStartingWith(datasourceName, bName );
			score = handleExtractionResult(brand, datasourceName, score, results);
			if (null != score) {				
				logger.info("Score found with a prefix resolution for brand {} and datasource {}", brand, datasourceName);
			}
		} 
		
		if (null == score) {
			// Trying a contains resolution
			logger.info("Trying a contains resolution for brand {} and datasource {}", brand, datasourceName);
			results = brandRepository.findByDatasourceNameAndBrandNameLike(datasourceName, bName);
			score = handleExtractionResult(brand, datasourceName, score, results);
			if (null != score) {
				logger.info("Score found with a contains resolution for brand {} and datasource {}", brand,	datasourceName);
			}
		}
		
		if (null == score) {
			//TODO : Log to vertical reporter
			logger.warn("No score found for brand {} and datasource {}, was {} possibilities", brand, datasourceName,results.size());
		} else {
			logger.info("Score found for brand {} and datasource {} : {}", brand, datasourceName, score);
		}
		return score;
	}

	private Double handleExtractionResult(String brand, String datasourceName, Double score, List<BrandScore> results) {
		if (results.size() == 1) {
			score = Double.valueOf(results.get(0).getNormalized());
		} else if (results.size() > 1) {
			// TODO : log to vertical reporter
			logger.warn("Multiple companies found for brand {} and datasource {}. Please consider a specific matching defintion in the vertical", brand, datasourceName);
			logger.warn("\n  - {}\n    - {}", brand, StringUtils.join(results.stream().map(e->e.getBrandName()).toArray(), "\n    - "));
			logger.warn("");
		} else {
			logger.warn("No score found for brand {} and datasource {}", brand, datasourceName);
		}
		return score;
	}

}