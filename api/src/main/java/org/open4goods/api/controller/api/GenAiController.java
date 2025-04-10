

package org.open4goods.api.controller.api;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.open4goods.commons.model.constants.RolesConstants;
import org.open4goods.commons.services.VerticalsConfigService;
import org.open4goods.model.ai.AiReview;
import org.open4goods.model.exceptions.ResourceNotFoundException;
import org.open4goods.model.product.Product;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.services.productrepository.services.ProductRepository;
import org.open4goods.services.prompt.dto.BatchPromptResponse;
import org.open4goods.services.prompt.service.BatchPromptService;
import org.open4goods.services.prompt.service.PromptService;
import org.open4goods.services.reviewgeneration.service.ReviewGenerationService;
import org.open4goods.services.serialisation.exception.SerialisationException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;

/**
 * This controller allows informations and communications about DatasourceConfigurations
 * TODO : Should split, into scoringController / resourceController, ....
 * @author goulven
 *
 */
@RestController
@PreAuthorize("hasAuthority('"+RolesConstants.ROLE_ADMIN+"')")
public class GenAiController {


	private PromptService aiService;
	private BatchPromptService batchAiService;
	private ProductRepository repository;
	private VerticalsConfigService verticalsConfigservice;
	private ReviewGenerationService reviewGenerationService;
	

	
	private VerticalsConfigService verticalsConfigService;


	public GenAiController(PromptService aiService,  VerticalsConfigService verticalsConfigService, BatchPromptService batchAiService, ProductRepository repository, VerticalsConfigService verticalsConfigservice, ReviewGenerationService reviewGenerationService) {
		this.aiService = aiService;
		this.verticalsConfigService = verticalsConfigService;
		this.batchAiService = batchAiService;
		this.repository = repository;
		this.verticalsConfigservice = verticalsConfigService;
		this.reviewGenerationService = reviewGenerationService;
	}
	
	
	
	@GetMapping("/prompt/json")
	@Operation(summary="Launch prompt")
	public Map<String, Object> promptJson(@RequestParam(defaultValue = "test") String key, 
			@RequestParam Map<String,Object> context) throws ResourceNotFoundException, SerialisationException {
		
		return aiService.jsonPrompt(key, context).getBody();
	}
	
	@GetMapping("/prompt/text")
	@Operation(summary="Launch prompt")
	public String prompt(@RequestParam(defaultValue = "test") String key, 
			@RequestParam Map<String,Object> context) throws ResourceNotFoundException, SerialisationException {
		
		return aiService.prompt(key, context).getRaw();
	}
	
	@GetMapping("/review/batch")
	@Operation(summary = "Launch batch review generation")
	public BatchPromptResponse<AiReview> batchReview(
	        @RequestParam(defaultValue = "tv") String vertical,
	        @RequestParam(defaultValue = "2") Integer numberOfProducts)
	        throws ResourceNotFoundException, SerialisationException {

	    VerticalConfig verticalConfig = verticalsConfigservice.getConfigById(vertical);
	    Stream<Product> productsStream = repository.exportVerticalWithValidDate(verticalConfig, false);
	    List<Product> products = productsStream.filter(e -> e.getReviews().size() == 0)
	            .limit(numberOfProducts)
	            .toList();
	    return reviewGenerationService.generateReviewBatch(products, verticalConfig);
	}
	
	
	
	
}
