package org.open4goods.ui.controllers.api;

import java.io.IOException;

import org.open4goods.commons.model.constants.RolesConstants;
import org.open4goods.commons.services.VerticalsConfigService;
import org.open4goods.model.exceptions.ResourceNotFoundException;
import org.open4goods.model.product.Product;
import org.open4goods.services.productrepository.services.ProductRepository;
import org.open4goods.services.reviewgeneration.dto.ProcessStatus;
import org.open4goods.services.reviewgeneration.service.ReviewGenerationService;
import org.open4goods.ui.config.yml.UiConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
/**
 * This controller maps the product page
 *
 * @author gof
 *
 */
public class ReviewController  {

	private static final Logger LOGGER = LoggerFactory.getLogger(ReviewController.class);

	private @Autowired UiConfig config;
	
	private @Autowired ProductRepository productRepository;

	private @Autowired VerticalsConfigService verticalConfigService;

	
	private @Autowired ReviewGenerationService reviewGenerationService;

	
	@PostMapping(path = {"/{vertical}/{id:\\d+}-*/review","/{id:\\d+}-*/review"}   )
	// 8806091548818
	// TODO : Authorization
	//@PreAuthorize("hasAuthority('"+RolesConstants.ROLE_XWIKI_ALL+"')")
	public long generateReview(@PathVariable Long id, @PathVariable(required = false) String vertical) throws IOException, ResourceNotFoundException {
		Product product = productRepository.getById(id);
		
		long ret = reviewGenerationService.generateReviewAsync(product, verticalConfigService.getConfigById(product.getVertical()));
		return ret;
	}

	
	
	@GetMapping(path = {"/{vertical}/{id:\\d+}-*/review","/{id:\\d+}-*/review"}   )
	@PreAuthorize("hasAuthority('"+RolesConstants.ROLE_XWIKI_ALL+"')")
	public ProcessStatus generateReviewStatus(@PathVariable Long id, @PathVariable(required = false) String vertical) throws IOException, ResourceNotFoundException {
		
		ProcessStatus ret = reviewGenerationService.getProcessStatus(id);
		return ret;
	}

	
	
}