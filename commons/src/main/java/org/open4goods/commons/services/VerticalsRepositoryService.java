package org.open4goods.commons.services;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

import org.open4goods.commons.config.yml.ui.VerticalConfig;
import org.open4goods.commons.model.product.Product;
import org.open4goods.commons.model.product.VerticalizedProduct;
import org.open4goods.commons.store.repository.VerticalizedProductIndexationWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Service responsible for dynamic creation of specific vertical indexes. This
 * service handles the index creation, settings, and mappings for different
 * verticals.
 * 
 * Improvements: - Detailed comments added for better understanding. - Added
 * proper logging for better traceability. - Improved method signature and
 * parameter validation. - Enhanced index creation flow with necessary checks.
 */
@Service
public class VerticalsRepositoryService {

	private static final Logger logger = LoggerFactory.getLogger(VerticalsRepositoryService.class);

	// Create an ObjectMapper used to build json progrmaticaly
	ObjectMapper mapper = new ObjectMapper();

	// Used to adress store R/W
	private ElasticsearchTemplate elasticsearchRestTemplate;
	// Used to derivate schema from Icecat FeatureGroupsTaxonomy
	private IcecatService icecatService;
	// Used for verticals configuration options
	private VerticalsConfigService verticalsConfigService;

	// The file queue implementation that buffers the VerticalisedProduct to be stored
	// TODO(p3,conf) : Limit from conf
	private BlockingQueue<VerticalizedProduct> verticalizedProductQueue = new LinkedBlockingQueue<>(500);
	
	
	
	
	/**
	 * Constructor for VerticalsRepositoryService.
	 *
	 * @param elasticOperations     Elasticsearch operations instance for
	 *                              interacting with Elasticsearch.
	 * @param elasticsearchTemplate
	 */
	public VerticalsRepositoryService(ElasticsearchTemplate elasticsearchTemplate, VerticalsConfigService verticalsConfigService, IcecatService icecatService) {
		this.elasticsearchRestTemplate = elasticsearchTemplate;
		this.icecatService = icecatService;
		this.verticalsConfigService = verticalsConfigService;
		
		// Indexes initialization
		initIndexes();
		
		
		// TODO(p2,conf) : from conf
		int vDequeueSize = 100;
		int vWorkers = 1;
		int vPauseDuration = 5000;
		// Starting batch indexation threads for verticalisedProducts
		// TODO : From conf
		for (int i = 0; i < vWorkers; i++) {			
			Thread.startVirtualThread((new VerticalizedProductIndexationWorker(this, vDequeueSize, vPauseDuration,"verticalized-dequeue-worker-"+i)));
		}
	}

	public void initIndexes() {
		for (VerticalConfig vConf : verticalsConfigService.getConfigsWithoutDefault()) {
			try {
				createIndex(vConf);
			} catch (Exception e) {
				logger.error("Error while creating index",e);
			}
		}
	}

	public void createIndex(VerticalConfig vConfig) throws IOException {

		
		// TODO : Clean the name
		String indexName = vConfig.indexName();
		IndexOperations indexOperations = elasticsearchRestTemplate.indexOps(IndexCoordinates.of(vConfig.indexName()));
		
		// TODO : Find the correspondinf attributes coverage
		
		
		// Checking index exists
		if (indexOperations.exists()) {
			logger.info("Index already exists, skipped : {} ", indexName);
		} else {

			logger.info("Index does not exists, will be created : {} ", indexName);
			
		
			Map<String, Object> settings = indexOperations.createSettings(VerticalizedProduct.class);
			Document mapping = indexOperations.createMapping(VerticalizedProduct.class);
			
			indexOperations.create(settings, mapping);
			
		}
	}
	
	
	public  VerticalizedProduct toVerticalizedProduct(Product source) {
		
		VerticalConfig v = verticalsConfigService.getConfigById(source.getVertical());
		
		VerticalizedProduct target = new VerticalizedProduct();
		
        // Copying individual attributes via getters and setters
        target.setId(source.getId());
        target.setExternalIds(source.getExternalIds());
        target.setCreationDate(source.getCreationDate());
        target.setLastChange(source.getLastChange());
        target.setVertical(source.getVertical());
        target.setExcluded(source.isExcluded());
        target.setAltModels(new HashSet<>(source.getAltModels()));
        target.setAltBrands(new HashMap<>(source.getAltBrands()));
        target.setNames(source.getNames());
        target.setAttributes(source.getAttributes());
        target.setPrice(source.getPrice());
        target.setDatasourceNames(new HashSet<>(source.getDatasourceNames()));
        target.setResources(new HashSet<>(source.getResources()));
        target.setCoverImagePath(source.getCoverImagePath());
        target.setGenaiTexts(source.getGenaiTexts());
        target.setGtinInfos(source.getGtinInfos());
        target.setGoogleTaxonomyId(source.getGoogleTaxonomyId());
        target.setCategories(new HashSet<>(source.getCategories()));
        target.setDsCategories(new HashMap<>(source.getDsCategories()));
        target.setScores(new HashMap<>(source.getScores()));
        target.setRanking(source.getRanking());
        target.setOffersCount(source.getOffersCount());

        return target;
	
	}

	
	/**
	 * Index the products in the associated vertical
	 * @param buffer
	 */
	public void index(Set<VerticalizedProduct> buffer) {
		
		// Splitting into the different categories
		Map<String, Set<VerticalizedProduct>> bag = buffer.stream()
				.filter(e-> null!=e.getVertical())
		        .collect(Collectors.groupingBy(
		                VerticalizedProduct::getVertical,  
		                Collectors.toSet()                 
		        ));

		// Indexing into given index
		
		bag.entrySet().forEach(e -> {
			VerticalConfig vc = verticalsConfigService.getConfigById(e.getKey());
			
			//TODO (perf,p3) : cache the index coordinates
			elasticsearchRestTemplate.
			save(e.getValue(), IndexCoordinates.of(vc.indexName()));
			
		});		
	}

	
	public void queueForVerticalisation(Product p) {
		
		if (null != p.getVertical()) {
			try {
				verticalizedProductQueue.put(toVerticalizedProduct(p));
			} catch (InterruptedException e) {
				logger.error("Interrupted while adding serialized to queue");
			}
		}
		
	}
	
	
	
	public BlockingQueue<VerticalizedProduct> getVerticalizedProductQueue() {
		return verticalizedProductQueue;
	}

	public void setVerticalizedProductQueue(BlockingQueue<VerticalizedProduct> verticalizedProductQueue) {
		this.verticalizedProductQueue = verticalizedProductQueue;
	}


	
}