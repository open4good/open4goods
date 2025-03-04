package org.open4goods.commons.services;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.open4goods.commons.config.yml.datasource.DataSourceProperties;
import org.open4goods.commons.helper.GenericFileLogger;
import org.open4goods.commons.model.data.BrandScore;
import org.open4goods.commons.store.repository.elastic.BrandScoresRepository;
import org.open4goods.model.constants.CacheConstants;
import org.slf4j.Logger;
import org.springframework.cache.annotation.Cacheable;

import ch.qos.logback.classic.Level;

/**
 * This service handles brands to companies resolution. It also allows a brand "aka names"  matching.
 * It is based on the open mappings defined here : https://github.com/open4good/brands-company-mapping
 * 
 * @author Goulven.Furet
 */
public class BrandScoreService {

	private  final Logger logger ;

	private BrandScoresRepository brandRepository;



	public BrandScoreService(  BrandScoresRepository brandRepository, String logsFolder) {
		this.brandRepository = brandRepository;
		this.logger = 	GenericFileLogger.initLogger("brand-service", Level.INFO, logsFolder);
	}

	
	/**
	 * Create or update a score for a given brand
	 * 
	 * @param brand
	 * @param datasourceProperties
	 * @param scoreValue
	 * @param url 
	 */
	public void addBrandScore(String brand, DataSourceProperties datasourceProperties, String scoreValue, String url) {

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

		BrandScore brandScore = new BrandScore(datasourceProperties, brand, scoreValue, url);
		

		
		
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
	 *TODO(p1,design) : review on exact match 
	 * Operates a brand name resolution against the brandscore repository, 
	 * if successfull retrieve the associated score
	 * @param brand
	 * @param string
	 * @return
	 */
	@Cacheable(keyGenerator = CacheConstants.KEY_GENERATOR, cacheNames = CacheConstants.ONE_HOUR_LOCAL_CACHE_NAME)
	public BrandScore getBrandScore(String brand, String datasourceName) {
			
			brand = brand.trim().toLowerCase();
			
			// 1 : Trying a direct resolution
			// TODO : With a find byid
			
			List<BrandScore> results = new ArrayList<>();
			String id = BrandScore.id(datasourceName, brand);
			
			BrandScore result = brandRepository.findById(id).orElse(null);		
			
			if (null == result) {
				//TODO : Log to vertical reporter
				logger.warn("No score found for brand {} and datasource {}, was {} possibilities", brand, datasourceName,results.size());
			} else {
				logger.info("Score found for brand {} and datasource {} : {}", brand, datasourceName, result.getNormalized());
			}
			return result;
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