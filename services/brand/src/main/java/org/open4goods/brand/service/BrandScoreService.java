package org.open4goods.brand.service;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.open4goods.brand.model.BrandScore;
import org.open4goods.brand.repository.BrandScoresRepository;
import org.open4goods.model.constants.CacheConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;

/**
 * Service responsible for persisting and retrieving brand scores.
 */
public class BrandScoreService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BrandScoreService.class);

    private final BrandScoresRepository brandRepository;

    public BrandScoreService(BrandScoresRepository brandRepository) {
        this.brandRepository = brandRepository;
    }

    public void addBrandScore(String brand,
                              String datasourceName,
                              Double invertScaleBase,
                              String scoreValue,
                              String url) {
        if (StringUtils.isEmpty(brand) || StringUtils.isEmpty(scoreValue)) {
            LOGGER.info("Cannot proceed empty brand or score, skipping");
            return;
        }

        LOGGER.info("Adding brand score {}:{} for brand {}", datasourceName, scoreValue, brand);
        BrandScore brandScore = new BrandScore(datasourceName, invertScaleBase, brand, scoreValue, url);
        LOGGER.info("Saving brand {}", brandScore);
        brandRepository.save(brandScore);
    }

    @Cacheable(keyGenerator = CacheConstants.KEY_GENERATOR, cacheNames = CacheConstants.ONE_HOUR_LOCAL_CACHE_NAME)
    public BrandScore getBrandScore(String brand, String datasourceName) {
        String normalizedBrand = brand.trim().toLowerCase();
        List<BrandScore> results = new ArrayList<>();
        String id = BrandScore.id(datasourceName, normalizedBrand);
        BrandScore result = brandRepository.findById(id).orElse(null);

        if (result == null) {
            LOGGER.warn("No score found for brand {} and datasource {}, was {} possibilities", brand, datasourceName,
                    results.size());
        } else {
            LOGGER.info("Score found for brand {} and datasource {} : {}", brand, datasourceName, result.getNormalized());
        }
        return result;
    }
}
