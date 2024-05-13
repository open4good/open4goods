package org.open4goods.api.services.aggregation.services.realtime;

import java.io.IOException;

import org.open4goods.api.services.aggregation.AbstractAggregationService;
import org.open4goods.config.yml.ui.VerticalConfig;
import org.open4goods.exceptions.AggregationSkipException;
import org.open4goods.helper.IdHelper;
import org.open4goods.model.data.DataFragment;
import org.open4goods.model.data.Resource;
import org.open4goods.model.product.Product;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MediaAggregationService extends AbstractAggregationService{

	private static final Logger logger = LoggerFactory.getLogger(MediaAggregationService.class);

	//    private final  ImageMagickService imageService;
	//
	//    private final  ElasticsearchRestTemplate esTemplate;



	//    private final ImageClassificationService imageClassificationService;

	//    private final  ExecutorService executor;

	//private ResourceService resourceService;

	public MediaAggregationService(final Logger logger) {
		super(logger);
		//		this.imageService = imageService;
		//		this.esTemplate = esTemplate;
//		this.config = config;
		//		this.resourceService = resourceService;
		// Creating executor
		//		executor =  Executors.newFixedThreadPool(config.getResourcesConfig().getResourceDownloadConcurentThreads());
	}

	@Override
	public void onDataFragment(final DataFragment input, final Product output, VerticalConfig vConf) throws AggregationSkipException {

		for (final Resource r : input.getResources()) {

			// Adding standard tags
			r.addTag(input.getDatasourceName());

			r.setCacheKey(IdHelper .generateResourceId(r.getUrl()));

			output.getResources().add(r);
		}
	}


	@Override
	public void close() throws IOException {
		//		executor.shutdown();
	}

	@Override
	public void onProduct(Product data, VerticalConfig vConf) throws AggregationSkipException {
	}


}
