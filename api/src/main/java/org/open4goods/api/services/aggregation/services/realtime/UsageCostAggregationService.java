package org.open4goods.api.services.aggregation.services.realtime;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

import org.open4goods.api.services.aggregation.AbstractAggregationService;
import org.open4goods.commons.exceptions.AggregationSkipException;
import org.open4goods.model.attribute.IndexedAttribute;
import org.open4goods.model.datafragment.DataFragment;
import org.open4goods.model.product.Product;
import org.open4goods.model.vertical.VerticalConfig;
import org.slf4j.Logger;

/**
 * Aggregation service computing the yearly usage cost for a product based on
 * vertical configuration averages.
 *
 * <p>The value is persisted as an indexed attribute named {@code usageCostYear}
 * so it can be used as a numeric filter on category pages.</p>
 */
public class UsageCostAggregationService extends AbstractAggregationService
{

    private static final String ATTRIBUTE_KEY = "usageCostYear";
    private static final BigDecimal DAYS_PER_YEAR = BigDecimal.valueOf(365);
    private static final int COST_SCALE = 2;

    public UsageCostAggregationService(final Logger logger)
    {
        super(logger);
    }

    /**
     * Aggregates the yearly usage cost when receiving a data fragment.
     *
     * @param fragment incoming data fragment
     * @param aggregatedData product being aggregated
     * @param vConf vertical configuration
     * @return unused map, always {@code null}
     * @throws AggregationSkipException if aggregation must be aborted
     */
    @Override
    public Map<String, Object> onDataFragment(final DataFragment fragment, final Product aggregatedData,
            final VerticalConfig vConf) throws AggregationSkipException
    {
        onProduct(aggregatedData, vConf);
        return null;
    }

    /**
     * Computes the yearly usage cost based on the configured average hours per day
     * and the average kWh cost.
     *
     * @param data product to enrich
     * @param vConf vertical configuration
     * @throws AggregationSkipException if aggregation must be aborted
     */
    @Override
    public void onProduct(final Product data, final VerticalConfig vConf) throws AggregationSkipException
    {
        if (data == null || vConf == null) {
            return;
        }

        Double averageHoursPerDay = vConf.getAverageHoursPerDay();
        Double averageKwhCost = vConf.getAverageKwhCost();

        if (averageHoursPerDay == null || averageKwhCost == null) {
            dedicatedLogger.debug("Skipping usage cost calculation for product {}: missing averages.",
                    data.getId());
            return;
        }

        if (averageHoursPerDay <= 0 || averageKwhCost <= 0) {
            dedicatedLogger.debug("Skipping usage cost calculation for product {}: invalid averages.",
                    data.getId());
            return;
        }

        BigDecimal costPerYear = BigDecimal.valueOf(averageHoursPerDay)
                .multiply(DAYS_PER_YEAR)
                .multiply(BigDecimal.valueOf(averageKwhCost))
                .setScale(COST_SCALE, RoundingMode.HALF_UP);


        if (data.getAttributes() != null) {
            data.getAttributes().getIndexed().put(ATTRIBUTE_KEY,
                    new IndexedAttribute(ATTRIBUTE_KEY, costPerYear.toPlainString()));
        }
    }
}
