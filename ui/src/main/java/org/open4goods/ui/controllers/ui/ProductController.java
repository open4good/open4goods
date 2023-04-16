package org.open4goods.ui.controllers.ui;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.open4goods.dao.AggregatedDataRepository;
import org.open4goods.exceptions.ResourceNotFoundException;
import org.open4goods.model.Localised;
import org.open4goods.model.constants.ProviderType;
import org.open4goods.model.data.AffiliationToken;
import org.open4goods.model.data.Description;
import org.open4goods.model.product.AggregatedData;
import org.open4goods.model.product.AggregatedPrice;
import org.open4goods.services.SerialisationService;
import org.open4goods.ui.config.yml.UiConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;

import com.ibm.icu.util.ULocale;
import com.mashape.unirest.http.exceptions.UnirestException;

@Controller
/**
 * This controller maps the index page
 *
 * @author gof
 *
 */
public class ProductController extends AbstractUiController {

	private static final Logger LOGGER = LoggerFactory.getLogger(ProductController.class);

	// The siteConfig
	private @Autowired UiConfig config;
	private @Autowired AggregatedDataRepository esDao;

	private @Autowired SerialisationService serialisationService;
	
	
	
	//////////////////////////////////////////////////////////////
	// Mappings
	//////////////////////////////////////////////////////////////

	/**
	 * The Home page.
	 *
	 * @param request
	 * @param response
	 * @param updatedData 
	 * @return
	 * @throws IOException 
	 * @throws UnirestException
	 */

	@GetMapping("/*-{id:\\d+}")
	public ModelAndView product(@PathVariable String id, final HttpServletRequest request, HttpServletResponse response) throws IOException {

		// Getting the product name
		String path= URLEncoder.encode(request.getServletPath().substring(1));							

		
		// Retrieve the AggregatedData
		AggregatedData data;
		try {
				data = esDao.getById(id);

		} catch (ResourceNotFoundException e) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Produit " + request.getServletPath() + " introuvable !");
		}

		if (null == data) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Produit " + request.getServletPath() + " introuvable !");
		}
		
	
		// Sending 301 id no match with product name
		if (!path.equals(data.getNames().getName())) {
			response.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
			response.setHeader("Location", config.getBaseUrl(Locale.FRANCE) + data.getNames().getName());
			return null;
		}
		
		//TODO : in a service
		// Adding the affiliationTokens in all prices
		for (AggregatedPrice price : data.getPrice().getOffers()) {
			inferAffiliationToken(data, price);
		}
		
		// Adding the affiliationTokens in min and max price
		//TODO(gof) : could be in price aggregation
		inferAffiliationToken(data, data.getPrice().getMinPrice());
//		inferAffiliationToken(data, data.getPrice().getMaxPrice());	
		
		ModelAndView mv = defaultModelAndView(("product"), request);
		mv.addObject("product", data);
		
		// Adding the diplay country
		if (null != data.getGtinInfos().getCountry()) {
			mv.addObject("originCountry", new ULocale("",data.getGtinInfos().getCountry()).getDisplayCountry( new ULocale(request.getLocale().toString())));
		}
		
		// Adding the images resource 
		
		return mv;
	}

	
	@PostMapping("/*-{id:\\d+}")
	public ModelAndView updateProduct(@RequestParam String productTitle, @RequestParam String productDescription,  @PathVariable String id, final HttpServletRequest request, HttpServletResponse response) throws IOException {

		// Retrieve the AggregatedData
		AggregatedData data;
		try {
			data = esDao.getById(id);
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
		
		esDao.index(data, AggregatedDataRepository.MAIN_INDEX_NAME);
		
		return product(id, request, response);
		
	}
	/**
	 * Infer the affiliation token in an aggregated price
	 * 
	 * @param data
	 * @param price
	 * @throws IOException 
	 */
	private void inferAffiliationToken(AggregatedData data, AggregatedPrice price)  {

			try {
				AffiliationToken token = new AffiliationToken(price, data);
				String serToken = URLEncoder.encode(serialisationService.compressString(serialisationService.toJson(token)), Charset.defaultCharset());			
				price.setAffiliationToken(serToken);
			} catch (Exception e) {
				LOGGER.error("Error while generating affiliation token for {} : {}", data, e.getMessage());
			}		
	}

}