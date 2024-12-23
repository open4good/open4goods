package org.open4goods.api.services.completion;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.open4goods.api.config.yml.ApiProperties;
import org.open4goods.api.services.AbstractCompletionService;
import org.open4goods.commons.config.yml.ui.VerticalConfig;
import org.open4goods.commons.dao.ProductRepository;
import org.open4goods.commons.exceptions.ResourceNotFoundException;
import org.open4goods.commons.model.AiReview;
import org.open4goods.commons.model.AiSource;
import org.open4goods.commons.model.attribute.Attribute;
import org.open4goods.commons.model.constants.ReferentielKey;
import org.open4goods.commons.model.product.Product;
import org.open4goods.commons.model.product.ProductAttribute;
import org.open4goods.commons.model.product.SourcedAttribute;
import org.open4goods.commons.services.VerticalsConfigService;
import org.open4goods.commons.services.ai.GenAiService;
import org.open4goods.commons.services.ai.PromptResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient.CallResponseSpec;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

public class PerplexityAttributesCompletionService  extends AbstractCompletionService{

	protected static final Logger logger = LoggerFactory.getLogger(PerplexityAttributesCompletionService.class);

	private static final String PERPLEXITY_DATASOURCE_NAME = "perplexity.ai";

	// Markdown parser
	private Parser parser = Parser.builder().build();
	
	private GenAiService aiService;

	
	public PerplexityAttributesCompletionService( GenAiService aiService, ProductRepository dataRepository, VerticalsConfigService verticalConfigService, ApiProperties apiProperties) {
		// TODO(p3,design) : Should set a specific log level here (not "aggregation)" one)
		super(dataRepository, verticalConfigService, apiProperties.logsFolder(), apiProperties.aggLogLevel());		
		this.aiService = aiService;
	}

	public void processProduct(VerticalConfig vertical, Product data) {
		
		try {
			logger.info("Completing attributes for {}",data);
			completePerplexity(vertical, data);
		} catch (Exception e) {
			logger.error("Error while compelting reviews with perplexity for {}",data, e);
		}
	}

	/**
	 * Operates the complexity completion process
	 * @param vConf
	 * @param data
	 * @throws JsonParseException
	 * @throws JsonMappingException
	 * @throws ResourceNotFoundException
	 * @throws IOException
	 */
	private void completePerplexity(VerticalConfig vConf, Product data) throws JsonParseException, JsonMappingException, ResourceNotFoundException, IOException {
		
		Map<String,Object> context = new HashMap<>();
		
		// Templates context feeding
		context.put("VERTICAL_NAME", (vConf.getI18n().get("fr").getVerticalHomeTitle()));
		context.put("PRODUCT_NAME", data.shortestOfferName());
		context.put("PRODUCT_BRAND", data.brand());
		context.put("PRODUCT_MODEL", data.model());
		context.put("PRODUCT_GTIN", data.gtin());
		context.put("PRODUCT", data);
		
		StringBuilder sb = new StringBuilder();
		vConf.getAttributesConfig().getConfigs().forEach(attrConf -> {
			if (attrConf.getSynonyms().size()>0 ) {
				sb.append("        ").append("- ").append(attrConf.getKey()).append(" (").append(attrConf.getName().get("fr")).append(")").append("\n");				
			}
			
		});
		context.put("ATTRIBUTES", sb.toString());

		// AI Prompting
		PromptResponse<Map<String, Object>> response = aiService.jsonPrompt("perplexity-product-attributes", context);
		
		//
		
		
		System.out.println(response.getRaw());
		
		// Updating product attributes
		updateProductAttributes(response.getBody(), data, vConf);
		
		// Updating status
		data.getDatasourceCodes().put(this.getClass().getSimpleName(), System.currentTimeMillis());
		
	}

	
	
	
	
	 
	private void updateProductAttributes(Map<String, Object> body, Product data, VerticalConfig vConf) {

		for (Entry<String, Object> entry : body.entrySet()) {
			String key = entry.getKey().trim().toUpperCase();
			String value = entry.getValue().toString().trim();
			
			if (entry.getValue() instanceof String) {
				
				
				if (key.equalsIgnoreCase("inconnu")) {
					logger.info("Skipping perplexity attribute {}:{}", key,value);
				} else {
					logger.info("Handling perplexity attribute {}:{}", key,value);

					switch (key) {
					case "MODEL":
						// Safe datasource, forcing model name
						String old = data.model();
						data.getAttributes().getReferentielAttributes().put(ReferentielKey.MODEL, value);
						data.addModel(value);
						
						if (null != old) {
							data.addModel(old);
						}
						break;
					case "MARQUE":
						// Safe datasource, forcing model name
						data.addBrand(PERPLEXITY_DATASOURCE_NAME, value, null, null);
						break;
					default:

						ProductAttribute attr = data.getAttributes().getAll().get(key);

						if (null == attr) {
							// A first time match
							attr = new ProductAttribute();
							attr.setName(key);
						}

						Attribute attrS = new Attribute();
						attrS.setName(key);
						attrS.setRawValue(value);
						attr.addSourceAttribute(new SourcedAttribute(attrS, PERPLEXITY_DATASOURCE_NAME));

						data.getAttributes().getAll().put(key, attr);

						break;
					}
				}
			} else {
				logger.warn("Perplexity completion skipped for key {}, not a String, but a {} : {}", key,  entry.getValue().getClass().getSimpleName(),  value);
			}
		
		}
		
	}

	@Override
	public boolean shouldProcess(VerticalConfig vertical, Product data) {
		// TODO(p1, cost) : Review cache is working
		// TODO(p1, cost) : Add a filter on enough attributes
		if (data.getDatasourceCodes().containsKey(this.getClass().getSimpleName())) {
			logger.info("Skipping perplexity review gen service, already exists");
			return false;
		} else {
			return true;
		}
	}

	@Override
	public String getDatasourceName() {
		return this.getClass().getSimpleName();
	}
}
