package org.open4goods.api.services.completion;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.open4goods.api.config.yml.ApiProperties;
import org.open4goods.api.services.AbstractCompletionService;
import org.open4goods.commons.config.yml.ui.VerticalConfig;
import org.open4goods.commons.dao.ProductRepository;
import org.open4goods.commons.exceptions.ResourceNotFoundException;
import org.open4goods.commons.model.product.Product;
import org.open4goods.commons.services.VerticalsConfigService;
import org.open4goods.commons.services.ai.GenAiService;
import org.open4goods.commons.services.ai.PromptResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient.CallResponseSpec;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

public class GenAiCompletionService  extends AbstractCompletionService{

	protected static final Logger logger = LoggerFactory.getLogger(GenAiCompletionService.class);

	private GenAiService aiService;

	private ApiProperties apiProperties;
	
	
	public GenAiCompletionService( GenAiService aiService, ProductRepository dataRepository, VerticalsConfigService verticalConfigService, ApiProperties apiProperties) {
		// TODO(p3,design) : Should set a specific log level here (not "aggregation)" one)
		super(dataRepository, verticalConfigService, apiProperties.logsFolder(), apiProperties.aggLogLevel());		
		this.aiService = aiService;
		this.apiProperties = apiProperties;
	}

	public void processProduct(VerticalConfig vertical, Product data) {
		
		// TODO : Handle the cache mechanism
//		try {
//			completePerplexity(vertical, data);
//		} catch (JsonParseException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (JsonMappingException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (ResourceNotFoundException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		
//		
//		
		
		
		
//		
//		logger.info("AI text completion for {}", data.getId());
//		
//		if (vertical.getGenAiConfig().isEnabled()) {
//			// Operating genai completion
//			aiService.complete(data, vertical, false);
//			
//			// Sleeping, to avoid rate limitation
//			try {
//				Thread.sleep(apiProperties.getGenAiPauseDurationMs());
//			} catch (InterruptedException e) {
//				Thread.currentThread().interrupt();
//				logger.error("Thread interrupted during rate limiting", e);
//			}
//		}
	}

	private void completePerplexity(VerticalConfig vConf, Product data) throws JsonParseException, JsonMappingException, ResourceNotFoundException, IOException {
		
		Map<String,Object> context = new HashMap<>();
		
		
		StringBuilder sb = new StringBuilder();
		
		vConf.getAttributesConfig().getConfigs().forEach(e -> {
			// TODO : i18n
			sb.append("  -").append(e.getName().get("fr")).append("\n");
		});
		
		
		context.put("VERTICAL_NAME", (vConf.getI18n().get("fr").getVerticalHomeTitle()));
		context.put("PRODUCT_NAME", data.longestOfferName());
		context.put("PRODUCT_BRAND", data.brand());
		context.put("PRODUCT_MODEL", data.model());
		context.put("PRODUCT_GTIN", data.gtin());
		context.put("ATTRIBUTES", sb.toString());
		
		PromptResponse<CallResponseSpec> response = aiService.prompt("product-completion-perplexity", context);
		
		System.out.println(response.getRaw());
		
		
		
		
	}

	@Override
	public boolean shouldProcess(VerticalConfig vertical, Product data) {
		// The cache mechanism is handled at the AiCompletionService
		return true;
	}

	@Override
	public String getDatasourceName() {
		// No datasource name for this kind of completion
		return null;
	}
}
