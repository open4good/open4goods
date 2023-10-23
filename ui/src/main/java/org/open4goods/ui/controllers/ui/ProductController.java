package org.open4goods.ui.controllers.ui;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.open4goods.config.yml.ui.VerticalConfig;
import org.open4goods.dao.ProductRepository;
import org.open4goods.exceptions.ResourceNotFoundException;
import org.open4goods.model.Localised;
import org.open4goods.model.constants.ProviderType;
import org.open4goods.model.data.AffiliationToken;
import org.open4goods.model.data.Description;
import org.open4goods.model.product.AggregatedPrice;
import org.open4goods.model.product.Product;
import org.open4goods.services.BrandService;
import org.open4goods.services.SerialisationService;
import org.open4goods.services.VerticalsConfigService;
import org.open4goods.ui.config.yml.UiConfig;
import org.open4goods.ui.helper.UiHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;

import com.ibm.icu.util.ULocale;
import com.mashape.unirest.http.exceptions.UnirestException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Controller
/**
 * This controller maps the product page
 *
 * @author gof
 *
 */
public class ProductController extends AbstractUiController {

	private static final Logger LOGGER = LoggerFactory.getLogger(ProductController.class);

	private @Autowired UiConfig config;
	
	private @Autowired ProductRepository productRepository;

	private @Autowired SerialisationService serialisationService;

	private @Autowired VerticalsConfigService verticalConfigService;

	private @Autowired BrandService brandService;


	/**
	 * A product, associated with a vertical at the home level.
	 *
	 * @param request
	 * @param response
	 * @param updatedData
	 * @return
	 * @throws IOException
	 * @throws UnirestException
	 */


	@GetMapping("/{vertical}/{id:\\d+}-*")
	public ModelAndView productInVertical(@PathVariable String vertical, @PathVariable String id, final HttpServletRequest request, HttpServletResponse response) throws IOException {


		VerticalConfig language = verticalConfigService.getVerticalForPath(vertical);

		if (null == language) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Produit " + request.getServletPath() + " introuvable !");
		}

		return buildProductView(id, vertical,request, response);


	}
		
	/**
	 * A product, not associated with a vertical at the home level.
	 *
	 * @param request
	 * @param response
	 * @param updatedData
	 * @return
	 * @throws IOException
	 * @throws UnirestException
	 */
	
	@GetMapping("/{id:\\d+}-*")
	public ModelAndView product(@PathVariable String id, final HttpServletRequest request, HttpServletResponse response) throws IOException {

		
		ModelAndView ret = buildProductView(id, null, request, response);;
		
	
		// Testing if on a vertical, redirect if so
		Product product = (Product) ret.getModel().get("product");
		
		if (null != product && !StringUtils.isEmpty(product.getVertical())) {
			// TODO : I18n
			String vPath = verticalConfigService.getConfigById(product.getVertical()).get().getBaseUrl(Locale.FRANCE); 
			ModelAndView mv = new ModelAndView("redirect:/"  + vPath+ "/"+product.getNames().getName());
			mv.setStatus(HttpStatus.MOVED_PERMANENTLY);				
			return mv;			
		}			
				
		return ret;
	}

	/**
	 * Product rendering build logic
	 * @param id
	 * @param vertical
	 * @param request
	 * @param response
	 * @return
	 */
	private ModelAndView buildProductView(String id, String vertical, final HttpServletRequest request,
			HttpServletResponse response) {
		
		// Getting the product name
		String path= URLEncoder.encode(request.getServletPath().substring(1));
		
		
		// Retrieve the Product
		Product data;
		try {
			data = productRepository.getById(id);

		} catch (ResourceNotFoundException e) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Produit " + request.getServletPath() + " introuvable !");
		}

		if (null == data) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Produit " + request.getServletPath() + " introuvable !");
		}


		// Sending 301 id no match with product name

		if (null == vertical) {

			if (!path.equals(data.getNames().getName())) {
				response.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
				// TODO : I18n
				ModelAndView mv = new ModelAndView("redirect:/" + data.getNames().getName());
				mv.setStatus(HttpStatus.MOVED_PERMANENTLY);
				mv.addObject("product", data);
				return mv;
			}
		} else {
			if (!path.equals(vertical+ "%2F" + data.getNames().getName())) {
				ModelAndView mv = new ModelAndView("redirect:/"+ vertical+"/"+data.getNames().getName());
				mv.setStatus(HttpStatus.MOVED_PERMANENTLY);
				mv.addObject("product", data);
				return mv;
			}
		}


		// Adding the affiliationTokens in all prices
		for (AggregatedPrice price : data.getPrice().getOffers()) {
			inferAffiliationToken(request, data, price);
		}

		// Adding the affiliationTokens in min and max price
		inferAffiliationToken(request, data, data.getPrice().getMinPrice());
		//		inferAffiliationToken(data, data.getPrice().getMaxPrice());

		ModelAndView mv = null;


		mv = defaultModelAndView("product", request);



		mv.addObject("product", data);
		
		VerticalConfig verticalConfig = verticalConfigService.getVerticalForPath(vertical);
		mv.addObject("verticalConfig", verticalConfig);

		
		
		// Adding the diplay country
		if (null != data.getGtinInfos().getCountry()) {
			mv.addObject("originCountry", new ULocale("",data.getGtinInfos().getCountry()).getDisplayCountry( new ULocale(request.getLocale().toString())));
		}

		
		// Adding the brand informations
		mv.addObject("hasBrandLogo", brandService.hasLogo(data.brand()));
		
		// Adding the UiHelper class
		mv.addObject("helper", new UiHelper(request, verticalConfig));
		
		// Adding the images resource

		return mv;
	}


	/**
	 * Update a product, from human edition 
	 * @param productTitle
	 * @param productDescription
	 * @param id
	 * @param request
	 * @param response
	 * @return
	 * @throws IOException
	 */
	@PostMapping("/*-{id:\\d+}")
	public ModelAndView updateProduct(@RequestParam String productTitle, @RequestParam String productDescription,  @PathVariable String id, final HttpServletRequest request, HttpServletResponse response) throws IOException {

		// Retrieve the Product
		Product data;
		try {
			data = productRepository.getById(id);
		} catch (ResourceNotFoundException e) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Produit " + request.getServletPath() + " introuvable !");
		}

		// Updating
		data.getNames().setManualName(productTitle);

		Description description = new Description();
		//TODO(i18n)
		description.setContent(new Localised(productDescription, "fr"));
		description.setProviderType(ProviderType.HUMAN_REDACTED);

		data.setHumanDescription(description);

		productRepository.index(data, ProductRepository.MAIN_INDEX_NAME);

		return buildProductView(id, null,request, response);

	}
	
	
	
	/**
	 * Infer the affiliation token in an aggregated price
	 *
	 * @param data
	 * @param price
	 * @throws IOException
	 */
	private void inferAffiliationToken(HttpServletRequest  request, Product data, AggregatedPrice price)  {

		try {
			AffiliationToken token = new AffiliationToken( price, data);
			
			
			String serToken = URLEncoder.encode(serialisationService.compressString(serialisationService.toJson(token)), Charset.defaultCharset());
			price.setAffiliationToken(serToken);
		} catch (Exception e) {
			LOGGER.error("Error while generating affiliation token for {} : {}", data, e.getMessage());
		}
	}

}