package org.open4goods.api.services.aggregation.services.realtime;

import java.util.HashMap;
import java.util.Map;

import org.open4goods.api.services.aggregation.AbstractAggregationService;
import org.open4goods.commons.exceptions.AggregationSkipException;
import org.open4goods.model.datafragment.DataFragment;
import org.open4goods.model.helper.IdHelper;
import org.open4goods.model.product.Product;
import org.open4goods.model.resource.Resource;
import org.open4goods.model.vertical.VerticalConfig;
import org.slf4j.Logger;

/**
 * Consolidates media resources (images, PDFs, manuals, etc.) from incoming
 * {@link DataFragment}s into the product's resource set.
 *
 * <p>For each resource in the fragment:
 * <ul>
 *   <li>Sets the datasource name and computes a stable cache key.</li>
 *   <li>If a resource with the same URL already exists on the product, only its
 *       tags are updated (preserving previously fetched metadata).</li>
 *   <li>Otherwise the resource is added to the product.</li>
 * </ul>
 *
 * <p>The {@link #onProduct(Product, VerticalConfig)} hook removes any Icecat
 * resources that require authentication and therefore cannot be served anonymously.
 */
public class MediaAggregationService extends AbstractAggregationService {

	/**
	 * @param logger dedicated aggregation logger
	 */
	public MediaAggregationService(final Logger logger) {
		super(logger);
	}

	/**
	 * Merges the fragment's resources into the product, then removes protected
	 * Icecat URLs.
	 */
	@Override
	public void onDataFragment(final DataFragment input, final Product output,
			final VerticalConfig vConf) throws AggregationSkipException {

		Map<String, Resource> existing = new HashMap<>();
		output.getResources().forEach(r -> existing.put(r.getUrl(), r));

		for (final Resource r : input.getResources()) {
			r.setDatasourceName(input.getDatasourceName());
			r.setCacheKey(IdHelper.generateResourceId(r.getUrl()));

			Resource old = existing.get(r.getUrl());
			if (old != null) {
				old.setTags(r.getTags());
				old.setHardTags(r.getHardTags());
			} else {
				output.getResources().add(r);
			}
		}

		onProduct(output, vConf);
	}

	/** Removes protected Icecat URLs that cannot be fetched anonymously. */
	@Override
	public void onProduct(final Product data, final VerticalConfig vConf) throws AggregationSkipException {
		data.getResources().removeIf(r -> isProtectedIcecatUrl(r.getUrl()));
	}

	/**
	 * Returns {@code true} when the URL points to an Icecat resource that requires
	 * an authenticated session ({@code ?access} query parameter).
	 */
	private boolean isProtectedIcecatUrl(final String url) {
		if (url == null) {
			return false;
		}
		boolean protectedLink = url.contains("icecat.biz") && url.contains("?access");
		if (protectedLink) {
			dedicatedLogger.info("Removing icecat protected url : {}", url);
		}
		return protectedLink;
	}

}
