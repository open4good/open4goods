package org.open4goods.ui.controllers.api;

import java.util.concurrent.CompletableFuture;

import org.open4goods.commons.helper.IpHelper;
import org.open4goods.commons.services.VerticalsConfigService;
import org.open4goods.model.exceptions.ResourceNotFoundException;
import org.open4goods.model.product.Product;
import org.open4goods.services.captcha.service.HcaptchaService;
import org.open4goods.services.productrepository.services.ProductRepository;
import org.open4goods.services.reviewgeneration.dto.ReviewGenerationStatus;
import org.open4goods.services.reviewgeneration.service.ReviewGenerationService;
import org.open4goods.ui.config.yml.UiConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;

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

	

	private @Autowired HcaptchaService captchaService;

	
	// TODO : have to validate captcha
	
	
	// 8806091548818
	// TODO : Authorization
	@PostMapping(path = {"/review/{id}"}   )
	
//	@PreAuthorize("hasAuthority('"+RolesConstants.ROLE_XWIKI_ALL+"')")
	public long generateReview(@PathVariable Long id, @PathVariable(required = false) String vertical, @RequestParam(name="hcaptchaResponse") String recaptchaResponse, HttpServletRequest request) throws ResourceNotFoundException{
		try {
			
			SecurityContextHolder.getContext().getAuthentication();
			
			captchaService.verifyRecaptcha(IpHelper.getIp(request), recaptchaResponse);
			Product product = productRepository.getById(id);
			
			CompletableFuture<Void> externalTask = CompletableFuture.runAsync(() -> {
				// TODO : complete with icecat
//				iceCatService.completeAndIndexProduct(verticalConfigService.getConfigByIdOrDefault(data.getVertical()), data);
			});

			
			long ret = reviewGenerationService.generateReviewAsync(product, verticalConfigService.getConfigById(product.getVertical()), externalTask);
			return ret;
		} catch (SecurityException e) {
			// TODO Redirect with proper 403
			throw e;
		} catch (ResourceNotFoundException e) {
			// TODO  Redirect with proper 404
			e.printStackTrace();
			throw e;
		}
	}

	
	
	@GetMapping(path ={"/review/{id}"} )
//	@PreAuthorize("hasAuthority('"+RolesConstants.ROLE_XWIKI_ALL+"')")
	public ReviewGenerationStatus generateReviewStatus(@PathVariable Long id) {
		
		ReviewGenerationStatus ret = reviewGenerationService.getProcessStatus(id);
		return ret;
		
		
	}

	
	
}