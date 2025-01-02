package org.open4goods.api.services;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.open4goods.api.config.yml.ApiProperties;
import org.open4goods.api.services.completion.PerplexityMarkdownService;
import org.open4goods.commons.config.yml.ui.VerticalConfig;
import org.open4goods.commons.dao.ProductRepository;
import org.open4goods.commons.exceptions.ResourceNotFoundException;
import org.open4goods.commons.model.AiReview;
import org.open4goods.commons.model.AiSourcedPage;
import org.open4goods.commons.model.product.Product;
import org.open4goods.commons.services.VerticalsConfigService;
import org.open4goods.commons.services.ai.GenAiService;
import org.open4goods.commons.services.ai.PromptResponse;
import org.open4goods.commons.store.repository.elastic.VerticalPagesRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient.CallResponseSpec;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

public class PageGenerationService {

	protected static final Logger logger = LoggerFactory.getLogger(PageGenerationService.class);


	private GenAiService aiService;
	private PerplexityMarkdownService perplexityMarkdownService;
	private VerticalPagesRepository pagesRepository;
	
	public PageGenerationService( GenAiService aiService, VerticalsConfigService verticalConfigService, ApiProperties apiProperties, PerplexityMarkdownService perplexityMarkdownService, VerticalPagesRepository pagesRepository) {
		// TODO(p3,design) : Should set a specific log level here (not "aggregation)" one)
		this.perplexityMarkdownService = perplexityMarkdownService;
		this.aiService = aiService;
		this.pagesRepository = pagesRepository;
	}


	/**
	 * Operates the complexity completion process
	 * @param vConf
	 * @param data
	 * @param question 
	 * @param title 
	 * @throws JsonParseException
	 * @throws JsonMappingException
	 * @throws ResourceNotFoundException
	 * @throws IOException
	 */
	public AiSourcedPage  generatePage(VerticalConfig vConf, String question, String id, String language, String title) throws JsonParseException, JsonMappingException, ResourceNotFoundException, IOException {
		
		Map<String,Object> context = new HashMap<>();
		
		// Templates context feeding
		context.put("VERTICAL_NAME", (vConf.getI18n().get("fr").getVerticalHomeTitle()));
		context.put("QUESTION", question);
		
		// AI Prompting
		PromptResponse<CallResponseSpec> response = aiService.prompt("perplexity-top-page-generation", context);
		
		System.out.println(response.getRaw());
		
		AiSourcedPage page = new AiSourcedPage();
		page.setId(id);
		page.setLanguage(language);
		page.setQuestion(question);
		page.setTitle(title);
		
		String[] frags  = response.getRaw().split("\n##");
		page.setAdvices(perplexityMarkdownService.parsebloc(frags[0]));
		
		
//		review.setSources(perplexityMarkdownService.parsesources(frags[5]));
		
		// TODO(p2, i18n) : internationalisation

		return page;
	}

	 
	
}
