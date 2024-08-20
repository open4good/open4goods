package org.open4goods.services;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.open4goods.config.yml.ui.VerticalConfig;
import org.open4goods.model.data.FeatureGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

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
	}

	public void initIndexes() {
		for (VerticalConfig vConf : verticalsConfigService.getConfigsWithoutDefault()) {
			try {
				createIndex(vConf);
			} catch (IOException e) {
				// TODO Auto-generated catch blockvConf
				e.printStackTrace();
			}
		}
	}

	public void createIndex(VerticalConfig vConfig) throws IOException {

		
		Map<String, String> features = icecatService.types(vConfig);
		
		// TODO : Clean the name
		String indexName = vConfig.indexName();
		IndexOperations indexOperations = elasticsearchRestTemplate.indexOps(IndexCoordinates.of(vConfig.indexName()));
		
		// TODO : Find the correspondinf attributes coverage
		
		
		// Checking index exists
		if (indexOperations.exists()) {
			logger.info("Index already exists, skipped : {} ", indexName);
		} else {

			logger.info("Index does not exists, will be created : {} ", indexName);
			
			// Create the JSON structure programmatically
			ObjectNode rootNode = mapper.createObjectNode();

			// Add the analysers configuration
			configureAnalyser(rootNode);

			///////////////////////////////////////
			// Add the mapping properties
			///////////////////////////////////////
			ObjectNode propertiesNode = mapper.createObjectNode();

			// Add the generic fields

			// TODO : Type and names as const
			addFieldMapping(propertiesNode, "offersCount", "integer", false);
			
			// TODO : Fix analyser
//			addFieldMapping(propertiesNode, "name", "text", "insensitiv", true, false);
			addFieldMapping(propertiesNode, "name", "text" , true);

			rootNode.set("properties", propertiesNode);

			// Convert ObjectNode to JSON string
			String jsonString = mapper.writeValueAsString(rootNode);

			String mappingJson = jsonString; // Pass json string here
			Document mapping = Document.parse(mappingJson);

			// Configure settings for the index (can be customized)
			Map<String, Object> settings = new HashMap<>();
			settings.put("index.number_of_shards", 1);
			settings.put("index.number_of_replicas", 1);

			indexOperations.create(settings, mapping);
			indexOperations.refresh(); // (Optional) refreshes the doc count
		}
	}

	/**
	 * Add a field to the mapping properties
	 * 
	 * @param propertiesNode
	 * @param name
	 * @param type
	 * @param analyser
	 * @param index
	 * @param store
	 */
	private void addFieldMapping(ObjectNode propertiesNode, String name, String type, String analyser, Boolean index, Boolean store) {
		ObjectNode field1Node = new ObjectMapper().createObjectNode();
		field1Node.put("type", type);

		// Optional: Set the analyzer if provided
		if (analyser != null && !analyser.isEmpty()) {
			field1Node.put("analyzer", analyser);
		}

		// Optional: Set the index option if provided
		if (index != null) {
			field1Node.put("index", index);
		}

		// Optional: Set the store option if provided
		if (store != null) {
			field1Node.put("store", store);
		}

		// Add this field's mapping to the properties node
		propertiesNode.set(name, field1Node);
	}

	/**
	 * Add a field to the mapping properties
	 * 
	 * @param propertiesNode
	 * @param name
	 * @param type
	 * @param analyser
	 * @param index
	 */
	private void addFieldMapping(ObjectNode propertiesNode, String name, String type, String analyser, Boolean index) {
		addFieldMapping(propertiesNode, name, type, analyser, index, false);
	}

	/**
	 * Add a field to the mapping properties
	 * 
	 * @param propertiesNode
	 * @param name
	 * @param type
	 * @param index
	 */
	private void addFieldMapping(ObjectNode propertiesNode, String name, String type, Boolean index) {
		addFieldMapping(propertiesNode, name, type, null, index, false);
	}

	/**
	 * Create the analyser part
	 * 
	 * @param rootNode
	 * @throws JsonProcessingException 
	 * @throws JsonMappingException 
	 */
	private void configureAnalyser(ObjectNode rootNode) throws JsonMappingException, JsonProcessingException {
		String analyserJson = """
{
  "index": {
  	"refresh_interval": "30s"
  },
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
				""";

			JsonNode analysisNode = mapper.readTree(analyserJson);

			// TODO : Set analyser at the correct location
			//			rootNode.set("analysis", analysisNode.get("analysis"));

	}
}