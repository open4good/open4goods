package org.open4goods.api.services.aggregation.services.realtime;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.open4goods.api.services.aggregation.AbstractAggregationService;
import org.open4goods.commons.config.yml.ui.VerticalConfig;
import org.open4goods.commons.exceptions.AggregationSkipException;
import org.open4goods.commons.helper.IdHelper;
import org.open4goods.commons.model.data.DataFragment;
import org.open4goods.commons.model.data.Resource;
import org.open4goods.commons.model.product.Product;
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
	public  Map<String, Object> onDataFragment(final DataFragment input, final Product output, VerticalConfig vConf) throws AggregationSkipException {

		
		
		Map<String,Resource> olds = new HashMap<>();
		output.getResources().forEach(r -> {
			olds.put(r.getUrl(), r);
		});
		
		
		
		for (final Resource r : input.getResources()) {

			// Adding standard tags
			r.setDatasourceName(input.getDatasourceName());
			
			r.setCacheKey(IdHelper .generateResourceId(r.getUrl()));

			
			
			// a special case here. to update tags. 
			
			Resource old = olds.get(r.getUrl());
			
			if (null != old) {
				old.setTags(r.getTags());
				old.setHardTags(r.getHardTags());
			} else {
				output.getResources().add(r);				
			}
		}
		return null;
	}


	@Override
	public void close() throws IOException {
		//		executor.shutdown();
	}

	@Override
	public Map<String, Object> onProduct(Product data, VerticalConfig vConf) throws AggregationSkipException {
		return null;
	}


}
