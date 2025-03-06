package org.open4goods.api.services.completion;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.open4goods.api.config.yml.ApiProperties;
import org.open4goods.api.services.AbstractCompletionService;
import org.open4goods.commons.dao.ProductRepository;
import org.open4goods.commons.services.VerticalsConfigService;
import org.open4goods.model.ai.AiReview;
import org.open4goods.model.exceptions.ResourceNotFoundException;
import org.open4goods.model.product.Product;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.services.prompt.dto.PromptResponse;
import org.open4goods.services.prompt.service.PromptService;
import org.open4goods.services.serialisation.exception.SerialisationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient.CallResponseSpec;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

public class PerplexityReviewCompletionService  extends AbstractCompletionService{

	protected static final Logger logger = LoggerFactory.getLogger(PerplexityReviewCompletionService.class);


	private PromptService aiService;
	private PerplexityMarkdownService perplexityMarkdownService;
	
	public PerplexityReviewCompletionService( PromptService aiService, ProductRepository dataRepository, VerticalsConfigService verticalConfigService, ApiProperties apiProperties, PerplexityMarkdownService perplexityMarkdownService) {
		// TODO(p3,design) : Should set a specific log level here (not "aggregation)" one)
		super(dataRepository, verticalConfigService, apiProperties.logsFolder(), apiProperties.aggLogLevel());		
		this.perplexityMarkdownService = perplexityMarkdownService;
		this.aiService = aiService;
	}

	public void processProduct(VerticalConfig vertical, Product data) {
		
		try {
			logger.info("Completing reviews for {}",data);
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
	 * @throws SerialisationException 
	 * @throws IOException
	 */
	private void completePerplexity(VerticalConfig vConf, Product data) throws  ResourceNotFoundException, SerialisationException {
		
		Map<String,Object> context = new HashMap<>();
		
		// Templates context feeding
		context.put("VERTICAL_NAME", (vConf.getI18n().get("fr").getVerticalHomeTitle()));
		context.put("PRODUCT_NAME", data.shortestOfferName());
		context.put("PRODUCT_BRAND", data.brand());
		context.put("PRODUCT_MODEL", data.model());
		context.put("PRODUCT_GTIN", data.gtin());
		context.put("PRODUCT", data);
		

		// AI Prompting
		PromptResponse<CallResponseSpec> response = aiService.prompt("perplexity-product-review", context);
		
		AiReview review = new AiReview();
		String[] frags  = response.getRaw().split("\n##");
		review.setDescription(perplexityMarkdownService.parsebloc(frags[0]));
		review.setPros(perplexityMarkdownService.parsebloc(frags[1]));
		review.setCons(perplexityMarkdownService.parsebloc(frags[2]));
		review.setReview(perplexityMarkdownService.parsebloc(frags[3]));
		review.setDataQuality(perplexityMarkdownService.parsebloc(frags[4]));
		review.setSources(perplexityMarkdownService.parsesources(frags[5]));
		
		// TODO(p2, i18n) : internationalisation
		data.getAiReviews().put("fr", review);
		data.getDatasourceCodes().put(this.getClass().getSimpleName(), System.currentTimeMillis());
		
	}

	 
	 
	@Override
	public boolean shouldProcess(VerticalConfig vertical, Product data) {
		// TODO(p1, cost) : Review cache is working
		// TODO(p1, cost) : Add a filter on year or creationDate (do not process too much recent items)
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
