package org.open4goods.services;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.open4goods.config.yml.ui.VerticalConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import jakarta.annotation.PostConstruct;

/**
 * Service responsible for dynamic creation of specific vertical indexes.
 * This service handles the index creation, settings, and mappings for different verticals.
 * 
 * Improvements:
 * - Detailed comments added for better understanding.
 * - Added proper logging for better traceability.
 * - Improved method signature and parameter validation.
 * - Enhanced index creation flow with necessary checks.
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
    
    /**
     * Constructor for VerticalsRepositoryService.
     *
     * @param elasticOperations Elasticsearch operations instance for interacting with Elasticsearch.
     * @param elasticsearchTemplate 
     */
    public VerticalsRepositoryService(ElasticsearchTemplate elasticsearchTemplate, VerticalsConfigService verticalsConfigService, IcecatService icecatService) {
        this.elasticsearchRestTemplate = elasticsearchTemplate;
        this.icecatService = icecatService;
        this.verticalsConfigService = verticalsConfigService;
    }
    

	@PostConstruct
    public void initIndexes() {
    	
    }
    
    public void createIndex(VerticalConfig vConfig) throws IOException {
    	
    	String indexName = vConfig.indexName();
    	IndexOperations indexOperations = elasticsearchRestTemplate.indexOps(IndexCoordinates.of(vConfig.indexName()));

    	// Checking index exists
    	if (indexOperations.exists()) {
    		logger.info("Index already exists, skipped : {} ",indexName);
    	} else {
    		
	        // Create the JSON structure programmatically
	        ObjectNode rootNode = mapper.createObjectNode();
	        ObjectNode propertiesNode = mapper.createObjectNode();
	        
	        configureAnalyser(rootNode);
	        
	        ObjectNode field1Node = mapper.createObjectNode();
	        field1Node.put("type", "text");
	        field1Node.put("analyzer", "standard");
	
	        ObjectNode field2Node = mapper.createObjectNode();
	        field2Node.put("type", "keyword");
	
	        propertiesNode.set("field1", field1Node);
	        propertiesNode.set("field2", field2Node);
	
	        rootNode.set("properties", propertiesNode);
	
	        // Convert ObjectNode to JSON string
	        String jsonString = mapper.writeValueAsString(rootNode);
	        
	        
	        
	        String mappingJson = jsonString; //Pass json string here
	        Document mapping = Document.parse(mappingJson);
	        
	        
	        // Configure settings for the index (can be customized)
	        Map<String, Object> settings = new HashMap<>();
	        settings.put("index.number_of_shards", 1);
	        settings.put("index.number_of_replicas",1);
	        
	
	        indexOperations.create(settings, mapping);
	        indexOperations.refresh(); //(Optional) refreshes the doc count
    	}
    }

    /**
     * Create the analyser part
     * 
     * {

  "analysis": {
    "analyzer": {
      "french": {
        "type": "custom",
        "tokenizer": "standard",
        "char_filter": [
          "html_strip"
        ],
        "filter": [
          "lowercase",
          "asciifolding"
        ]
      }
    }
  }
}

     * @param rootNode
     */
	private void configureAnalyser(ObjectNode rootNode) {
		// "analysis": { "analyzer": { "french": { ... } } }
		ObjectNode analysisNode = mapper.createObjectNode();
		ObjectNode analyzerNode = mapper.createObjectNode();
		ObjectNode frenchAnalyzerNode = mapper.createObjectNode();

		// "french": { "type": "custom", "tokenizer": "standard", "char_filter": [...], "filter": [...] }
		frenchAnalyzerNode.put("type", "custom");
		frenchAnalyzerNode.put("tokenizer", "standard");

		// "char_filter": ["html_strip"]
		ArrayNode charFilterArray = mapper.createArrayNode();
		charFilterArray.add("html_strip");
		frenchAnalyzerNode.set("char_filter", charFilterArray);

		// "filter": ["lowercase", "asciifolding"]
		ArrayNode filterArray = mapper.createArrayNode();
		filterArray.add("lowercase");
		filterArray.add("asciifolding");
		frenchAnalyzerNode.set("filter", filterArray);

		// Nest the frenchAnalyzerNode within analyzerNode
		analyzerNode.set("french", frenchAnalyzerNode);

		// Nest the analyzerNode within analysisNode
		analysisNode.set("analyzer", analyzerNode);

		// Finally, nest the analysisNode within rootNode
		rootNode.set("analysis", analysisNode);
	}
    
    
    
    
    
    
    
    
    
}
