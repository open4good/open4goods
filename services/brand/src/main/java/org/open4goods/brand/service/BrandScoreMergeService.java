package org.open4goods.brand.service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.open4goods.brand.model.Brand;
import org.open4goods.brand.model.BrandScore;
import org.open4goods.brand.model.Company;
import org.open4goods.brand.model.CompanyScore;

/**
 * Merges the two brand-score planes:
 * <ul>
 *   <li>the <b>curated</b> plane: per-provider {@link CompanyScore} entries held in
 *       the git referential ({@link Company#getScores()}), maintained by the
 *       brands-maintenance agent;</li>
 *   <li>the <b>volatile</b> plane: live {@link BrandScore} documents continuously
 *       refreshed in Elasticsearch by the crawler (Sustainalytics today).</li>
 * </ul>
 * The volatile plane wins per provider: a fresh scraped score overlays the
 * curated snapshot.
 */
public class BrandScoreMergeService {

    private final BrandScoreService brandScoreService;

    public BrandScoreMergeService(BrandScoreService brandScoreService) {
        this.brandScoreService = brandScoreService;
    }

    /**
     * @param brand a resolved brand
     * @return per-provider merged company scores (curated overlaid with live ES scores)
     */
    public Map<String, CompanyScore> mergedScores(Brand brand) {
        Map<String, CompanyScore> merged = new LinkedHashMap<>();
        if (brand == null) {
            return merged;
        }

        Company company = brand.getCompany();
        if (company != null && company.getScores() != null) {
            merged.putAll(company.getScores());
        }

        String companyName = brand.getCompanyName();
        if (StringUtils.isNotBlank(companyName)) {
            for (BrandScore liveScore : brandScoreService.getBrandScores(companyName)) {
                String provider = providerKey(liveScore.getDatasourceName());
                if (provider != null) {
                    merged.put(provider, toCompanyScore(liveScore));
                }
            }
        }
        return merged;
    }

    /**
     * Maps a datasource name to a provider key (e.g. {@code sustainalytics.com}
     * to {@code sustainalytics}).
     */
    static String providerKey(String datasourceName) {
        if (StringUtils.isBlank(datasourceName)) {
            return null;
        }
        int dot = datasourceName.indexOf('.');
        return dot > 0 ? datasourceName.substring(0, dot) : datasourceName;
    }

    private CompanyScore toCompanyScore(BrandScore liveScore) {
        CompanyScore score = new CompanyScore();
        score.setRating(liveScore.getScoreValue());
        score.setValue(liveScore.getNormalized());
        score.setUrl(liveScore.getUrl());
        if (liveScore.getLastUpdate() > 0) {
            score.setRetrievedAt(Instant.ofEpochMilli(liveScore.getLastUpdate()).toString());
        }
        return score;
    }
}
