package org.open4goods.api.services.aggregation.services.batch.scores;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.open4goods.model.product.Product;
import org.open4goods.model.product.Score;
import org.open4goods.model.rating.Cardinality;
import org.open4goods.model.vertical.ImpactScoreConfig;
import org.open4goods.model.vertical.VerticalConfig;
import org.slf4j.LoggerFactory;

class EcoScoreAggregationServiceTest {

        @Test
        void doneShouldNotFailWhenAllEcoScoresMissing() {
                EcoScoreAggregationService service = new EcoScoreAggregationService(LoggerFactory.getLogger(getClass()));
                VerticalConfig config = verticalConfig(Map.of("S1", 1.0d));
                Product product = new Product();
                product.setId(1L);

                service.init(List.of(product));
                service.onProduct(product, config);

                assertThat(product.ecoscore()).isNull();

                assertThatCode(() -> service.done(List.of(product), config)).doesNotThrowAnyException();
                assertThat(product.getRanking().getGlobalCount()).isZero();
        }

        @Test
        void doneKeepsAbsoluteValuesAndRanksRealEcoScoresOnly() {
                EcoScoreAggregationService service = new EcoScoreAggregationService(LoggerFactory.getLogger(getClass()));
                VerticalConfig config = verticalConfig(Map.of("S1", 1.0d));

                Product validProduct = new Product();
                validProduct.setId(1L);
                validProduct.getScores().put("S1", score("S1", 42.0d));

                Product missingSubScore = new Product();
                missingSubScore.setId(2L);

                List<Product> products = List.of(validProduct, missingSubScore);
                service.init(products);
                products.forEach(product -> service.onProduct(product, config));
                service.done(products, config);

                assertThat(validProduct.ecoscore()).isNotNull();
                assertThat(validProduct.ecoscore().getRelativ().getValue()).isEqualTo(validProduct.ecoscore().getAbsolute().getValue());
                assertThat(validProduct.getRanking().getGlobalCount()).isEqualTo(1L);
                assertThat(validProduct.getRanking().getGlobalBest()).isEqualTo(validProduct.getId());
                assertThat(validProduct.getRanking().getGlobalBetter()).isNull();

                assertThat(missingSubScore.getRanking().getGlobalCount()).isZero();
                assertThat(missingSubScore.ecoscore()).isNotNull();
                assertThat(missingSubScore.ecoscore().getVirtual()).isTrue();
        }

        private VerticalConfig verticalConfig(Map<String, Double> weights) {
                VerticalConfig config = new VerticalConfig();
                ImpactScoreConfig impactScoreConfig = new ImpactScoreConfig();
                impactScoreConfig.setCriteriasPonderation(weights);
                config.setImpactScoreConfig(impactScoreConfig);
                return config;
        }

        private Score score(String name, double relativValue) {
                Score score = new Score(name, relativValue);
                Cardinality cardinality = new Cardinality();
                cardinality.setValue(relativValue);
                cardinality.setMin(relativValue);
                cardinality.setMax(relativValue);
                cardinality.setAvg(relativValue);
                cardinality.setCount(1);
                cardinality.setSum(relativValue);
                score.setRelativ(cardinality);
                return score;
        }
}
