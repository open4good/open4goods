package org.open4goods.api.services.aggregation.services.batch.scores;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.open4goods.model.exceptions.ValidationException;
import org.open4goods.model.product.Product;
import org.open4goods.model.product.EcoScoreRanking;
import org.open4goods.model.product.Score;
import org.open4goods.model.rating.Cardinality;
import org.open4goods.model.vertical.VerticalConfig;
import org.slf4j.Logger;

/**
 * Create an ecoscore based on existing scores, (see yaml config files)
 * @author goulven
 *
 */
public class EcoScoreAggregationService extends AbstractScoreAggregationService {

	private static final String ECOSCORE_SCORENAME = "ECOSCORE";

	public EcoScoreAggregationService(final Logger logger) {
		super(logger);
	}



	@Override
        public void onProduct(Product data, VerticalConfig vConf) {

                try {

                        if (null != vConf.getImpactScoreConfig() && vConf.getImpactScoreConfig().getCriteriasPonderation().size() > 0 ) {
                                // Compute the ecoscore from existing scores
                                Double score = generateEcoScore(data.getScores(),vConf);
                                if (score == null) {
                                        dedicatedLogger.warn("EcoScore rating skipped for {} due to missing sub-scores", data.getId());
                                        return;
                                }

                                // Processing cardinality
                                incrementCardinality(ECOSCORE_SCORENAME,score);

				// Saving the actual score in the product, it will be relativized after this batch (see super().done())
				Score s = new Score(ECOSCORE_SCORENAME, score);
				data.getScores().put(s.getName(),s);
			} else {
				dedicatedLogger.info("No ImpactScore defined for vertical",vConf);
			}
		} catch (ValidationException e) {
			dedicatedLogger.error("Ecoscore aggregation failed for {} : {}",data,e.getMessage());
		}
	}



        private Double generateEcoScore(Map<String, Score> scores, VerticalConfig vConf) throws ValidationException {


                double ecoscoreVal = 0.0;
                for (String config :  vConf.getImpactScoreConfig().getCriteriasPonderation().keySet()) {
                        Score score = scores.get(config);

                        if (null == score) {
                                dedicatedLogger.warn("EcoScore rating cannot proceed, missing subscore : {}", config);
                                return null;
                        }


                        Double value = resolveRelativeValue(config, score);
                        if (value == null) {
                                return null;
                        }

                        ecoscoreVal += value * Double.valueOf(vConf.getImpactScoreConfig().getCriteriasPonderation().get(config));
                }

                return ecoscoreVal;
        }


        private Double resolveRelativeValue(String config, Score score) {
                if (score.getRelativ() != null && score.getRelativ().getValue() != null) {
                        return score.getRelativ().getValue();
                }

                if (score.getAbsolute() != null && score.getAbsolute().getValue() != null) {
                        try {
                                return relativize(score.getAbsolute().getValue(), score.getAbsolute());
                        } catch (ValidationException e) {
                                dedicatedLogger.warn("EcoScore relativization failed for {} : {}", config, e.getMessage());
                                return null;
                        }
                }

                if (score.getValue() != null) {
                        dedicatedLogger.warn("EcoScore using raw value for {} due to missing cardinalities", config);
                        return score.getValue();
                }

                dedicatedLogger.warn("EcoScore rating cannot proceed, missing value for {}", config);
                return null;
        }


	@Override
	public void done(Collection<Product> datas, VerticalConfig vConf) {

                if (null == vConf.getImpactScoreConfig() || vConf.getImpactScoreConfig().getCriteriasPonderation().isEmpty()) {
                        dedicatedLogger.error("No ImpactScore defined for vertical", vConf);
                        return;
                }

                super.done(datas, vConf);

                List<Product> productsWithRealEcoScore = datas.stream()
                                .filter(this::hasRealEcoScore)
                                .toList();

                if (productsWithRealEcoScore.isEmpty()) {
                        dedicatedLogger.warn("{} -> No real ecoscore computed, skipping ranking", this.getClass().getSimpleName());
                        return;
                }

                // EcoScore stays on its absolute value (no relativisation)
                for (Product product : productsWithRealEcoScore) {
                        Cardinality absolute = product.ecoscore().getAbsolute();
                        product.ecoscore().setRelativ(new Cardinality(absolute));
                }

                ///////////////////////
                // EcoScore ranking and "best alternativ" reach
                ///////////////////////
                List<Product> sorted = new ArrayList<>(productsWithRealEcoScore);
                sorted.sort(Comparator.comparingDouble(p -> p.ecoscore().getRelativ().getValue()));

                int count = sorted.size();
                Long bestProductId = sorted.get(count - 1).getId();

                for (int i = 0; i < count; i++) {
                        Product product = sorted.get(i);
                        EcoScoreRanking ranking = ensureRanking(product);
                        ranking.setGlobalCount(count);
                        ranking.setGlobalPosition(count - i);
                        ranking.setGlobalBest(bestProductId);

                        if (i < count - 1) {
                                ranking.setGlobalBetter(sorted.get(i + 1).getId());
                        } else {
                                ranking.setGlobalBetter(null);
                        }

                }


        }


        private EcoScoreRanking ensureRanking(Product product) {
                EcoScoreRanking ranking = product.getRanking();
                if (null == ranking) {
                        ranking = new EcoScoreRanking();
                        product.setRanking(ranking);
                }
                return ranking;
        }

        private boolean hasRealEcoScore(Product product) {
                Score ecoscore = product.ecoscore();
                return ecoscore != null
                                && !Boolean.TRUE.equals(ecoscore.getVirtual())
                                && ecoscore.getAbsolute() != null
                                && ecoscore.getAbsolute().getValue() != null;
        }

}
