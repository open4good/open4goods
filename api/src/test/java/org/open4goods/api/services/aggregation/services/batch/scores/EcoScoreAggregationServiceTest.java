package org.open4goods.api.services.aggregation.services.batch.scores;

import org.junit.jupiter.api.Test;
import org.open4goods.model.product.Product;
import org.open4goods.model.product.Score;
import org.open4goods.model.rating.Cardinality;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.model.vertical.ImpactScoreConfig;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class EcoScoreAggregationServiceTest {

    @Test
    public void testEcoScoreOutput() {
        EcoScoreAggregationService service = new EcoScoreAggregationService(LoggerFactory.getLogger(EcoScoreAggregationServiceTest.class));

        Product p = new Product();
        p.setId(1L);

        // Setting up subscores
        Score cnrgy = new Score("CLASSE_ENERGY", 85.0);
        Cardinality cr = new Cardinality();
        cr.setValue(4.25); // B is around 4.25 / 5.0
        cnrgy.setRelativ(cr);
        p.getScores().put("CLASSE_ENERGY", cnrgy);

        Score wty = new Score("WARRANTY", 24.0);
        Cardinality wr = new Cardinality();
        wr.setValue(2.5); // Average
        wty.setRelativ(wr);
        p.getScores().put("WARRANTY", wty);

        VerticalConfig vConf = new VerticalConfig();
        vConf.setId("refrigerator");
        ImpactScoreConfig isc = new ImpactScoreConfig();
        Map<String, Double> pond = new HashMap<>();
        ponderations(pond);
        isc.setCriteriasPonderation(pond);
        vConf.setImpactScoreConfig(isc);

        List<Product> products = new ArrayList<>();
        products.add(p);

        service.init(products);
        service.onProduct(p, vConf);
        service.done(products, vConf);

        Score eco = p.getScores().get("ECOSCORE");
        assertNotNull(eco);
        System.out.println("TEST ECOSCORE VALUE IS: " + eco.getValue());
        System.out.println("TEST ECOSCORE ON20 IS: " + eco.on20());
    }

    private void ponderations(Map<String, Double> pond) {
        pond.put("WARRANTY", 0.3);
        pond.put("CLASSE_ENERGY", 0.35);
        pond.put("WEIGHT", 0.15);
        pond.put("BRAND_SUSTAINALYTICS_SCORING", 0.15);
        pond.put("DATA_QUALITY", 0.05);
    }
}
