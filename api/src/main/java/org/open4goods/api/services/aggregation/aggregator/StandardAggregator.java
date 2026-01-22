package org.open4goods.api.services.aggregation.aggregator;

import java.util.Collection;

import java.util.List;

import org.open4goods.api.services.aggregation.AbstractAggregationService;
import org.open4goods.commons.exceptions.AggregationSkipException;
import org.open4goods.model.datafragment.DataFragment;
import org.open4goods.model.product.Product;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.verticals.VerticalsConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The real time AggregationFacadeService is stateless and thread safe.
 * @author goulven
 *
 */
public class StandardAggregator extends AbstractAggregator {

	protected static final Logger logger = LoggerFactory.getLogger(StandardAggregator.class);
	private VerticalsConfigService verticalConfigService;


	public StandardAggregator(final List<AbstractAggregationService> services, VerticalsConfigService verticalConfigService) {
		super(services);
		this.verticalConfigService = verticalConfigService;
	}


	/**
	 * Increment the Product with the given DataFragment using the services registered on this aggregator
	 * @param datas
	 * @return
	 * @throws AggregationSkipException
	 */
	public Product onDatafragment(final DataFragment fragment, final Product data ) throws AggregationSkipException {

		logger.debug("Incrementing Product with {} DataFragment and using {} services",fragment,services.size());

		// Call transformation building registered service
		for (final AbstractAggregationService service : services) {
			try {
				VerticalConfig vConf = verticalConfigService.getConfigByIdOrDefault(data.getVertical());
				service.onDataFragment(fragment, data, vConf);
			}
			catch (AggregationSkipException e) {
				throw e;
			}
			catch (final Exception e) {
				logger.error("AggregationFacadeService {} throw an exception while processing data {}",service.getClass().getName(), data,e);
			}
		}
		return data;
	}

	/**
	 * update the Product using the services registered on this aggregator
	 * @param datas
	 * @return
	 * @throws AggregationSkipException
	 */
	public Product onProduct(final Product data ) throws AggregationSkipException {

		logger.debug("Updating product using {} services",services.size());

		// Call transformation building registered service
		for (final AbstractAggregationService service : services) {
			try {
				VerticalConfig vConf = verticalConfigService.getConfigByIdOrDefault(data.getVertical());
				service.onProduct( data, vConf);
			}
			catch (AggregationSkipException e) {
				throw e;
			}
			catch (final Exception e) {
				logger.error("AggregationFacadeService {} throw an exception while processing data {}",service.getClass().getName(), data,e);

			}
		}
		return data;
	}

	/**
	 * Batch processing entry point that respects lifecycle hooks.
	 *
	 * @param datas products to sanitize
	 * @param vConf vertical configuration to apply when provided
	 * @throws AggregationSkipException when a service explicitly skips aggregation
	 */
	public void onProducts(final Collection<Product> datas, final VerticalConfig vConf) throws AggregationSkipException
	{
		logger.debug("Updating {} products using {} services", datas.size(), services.size());

		for (final AbstractAggregationService service : services) {
			service.init(datas);
			for (final Product data : datas) {
				try {
					VerticalConfig resolvedConfig = vConf != null ? vConf
							: verticalConfigService.getConfigByIdOrDefault(data.getVertical());
					service.onProduct(data, resolvedConfig);
				} catch (AggregationSkipException e) {
					throw e;
				} catch (final Exception e) {
					logger.error("AggregationFacadeService {} throw an exception while processing data {}",
							service.getClass().getName(), data, e);
				}
			}
			service.done(datas, vConf);
		}
	}
}
