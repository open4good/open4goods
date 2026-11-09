package org.open4goods.api.services.aggregation.services.batch.scores;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.open4goods.model.exceptions.ValidationException;
import org.open4goods.model.product.Product;
import org.open4goods.model.product.Score;
import org.open4goods.model.rating.Cardinality;
import org.open4goods.model.vertical.AttributeConfig;
import org.open4goods.model.vertical.ImpactScoreConfig;
import org.open4goods.model.vertical.VerticalConfig;
import org.slf4j.Logger;

/**
 * Computes the {@code DATA_QUALITY} score from the amount of real (non virtual)
 * scores already present on the product. The service operates regardless of
 * whether optional metadata such as the brand are present so downstream score
 * composition (e.g. EcoScore) always receives a data-quality subscore.
 *
 * <p>In ImpactScore V2 ({@link ImpactScoreConfig#isV2Active()}), {@code DATA_QUALITY} is out of the ImpactScore and
 * becomes its confidence indicator: it counts only the real scores of the {@code asScore} criteria listed in
 * {@code availableImpactScoreCriterias}, without {@code BRAND_*} scores, itself or composites (design
 * {@code docs/impactscore/v2-design.md}, 2.3). V1 counts every real score.
 */
public class DataCompletion2ScoreAggregationService extends AbstractScoreAggregationService {

        private static final String DATA_QUALITY_SCORENAME = "DATA_QUALITY";

        /** Prefix of the brand scores ({@code BRAND_<PROVIDER>_SCORING}), not counted in V2. */
        private static final String BRAND_SCORE_PREFIX = "BRAND_";


        public DataCompletion2ScoreAggregationService(final Logger logger) {
                super(logger);
        }



        /**
         * Ensures each product receives a data-quality score that counts existing
         * non-virtual scores. Intentionally runs even when identifying attributes
         * (e.g. brand) are missing so the pipeline always has a data-quality signal.
         */
        @Override
        public void onProduct(Product data, VerticalConfig vConf) {

                try {
                        Double score = generateScoreFromDataquality(data.getScores(), vConf);

                        // Processing cardinality
                        incrementCardinality(DATA_QUALITY_SCORENAME, score, vConf);
                        Score s = new Score(DATA_QUALITY_SCORENAME, score);
                        s.setAbsolute(new Cardinality());
                        s.getAbsolute().setValue(score);
                        // Saving in product
                        data.getScores().put(s.getName(),s);
                } catch (ValidationException e) {
			dedicatedLogger.warn("DataQuality to score fail for {}",data,e);
		}
		
		
	}


        /**
         * The data score is the number of scores that are not virtuals: every one of them in V1, only those of the
         * ImpactScore criteria in V2.
         * @param map the product scores
         * @param vConf the vertical configuration
         * @return the number of counted real scores
         */
        private Double generateScoreFromDataquality(Map<String, Score> map, VerticalConfig vConf) {

                ImpactScoreConfig impactScoreConfig = vConf == null ? null : vConf.getImpactScoreConfig();
                if (impactScoreConfig != null && impactScoreConfig.isV2Active()) {
                        Set<String> criterias = v2CountedCriterias(vConf);
                        return Double.valueOf(map.values().stream()
                                        .filter(e -> !Boolean.TRUE.equals(e.getVirtual()))
                                        .filter(e -> criterias.contains(e.getName()))
                                        .count());
                }

                return  Double.valueOf(map.values().stream().filter(e -> !e.getVirtual()).filter(e -> !e.getName().equals(DATA_QUALITY_SCORENAME)) .count());

        }

        /**
         * The criteria {@code DATA_QUALITY} counts in V2: the {@code asScore} attributes of
         * {@code availableImpactScoreCriterias}, without {@code BRAND_*}, {@code DATA_QUALITY} and composites.
         * @param vConf the vertical configuration
         * @return the names of the counted scores
         */
        static Set<String> v2CountedCriterias(VerticalConfig vConf) {
                Set<String> criterias = new HashSet<>();
                if (vConf.getAvailableImpactScoreCriterias() == null || vConf.getAttributesConfig() == null) {
                        return criterias;
                }
                for (String key : vConf.getAvailableImpactScoreCriterias()) {
                        if (key == null || DATA_QUALITY_SCORENAME.equals(key) || key.startsWith(BRAND_SCORE_PREFIX)
                                        || (vConf.getCompositeScores() != null && vConf.getCompositeScores().contains(key))) {
                                continue;
                        }
                        AttributeConfig attribute = vConf.getAttributesConfig().getAttributeConfigByKey(key);
                        if (attribute != null && attribute.isAsScore()) {
                                criterias.add(key);
                        }
                }
                return criterias;
        }


}
