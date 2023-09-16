package org.open4goods.ui.controllers.ui;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.Locale;

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

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

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
	private @Autowired ProductRepository esDao;

	private @Autowired SerialisationService serialisationService;

	private @Autowired VerticalsConfigService verticalConfigService;

	private @Autowired BrandService brandService;

	//////////////////////////////////////////////////////////////
	// Mappings
	//////////////////////////////////////////////////////////////

	/**
	 * The product, at the home level.
	 *
	 * @param request
	 * @param response
	 * @param updatedData
	 * @return
	 * @throws IOException
	 * @throws UnirestException
	 */


	@GetMapping("/{vertical}/*-{id:\\d+}")
	public ModelAndView productInVertical(@PathVariable String vertical, @PathVariable String id, final HttpServletRequest request, HttpServletResponse response) throws IOException {


		VerticalConfig language = verticalConfigService.getVerticalForPath(vertical);

		if (null == language) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Produit " + request.getServletPath() + " introuvable !");
		}

		return product(id, vertical,request, response);


	}
		
	@GetMapping("/*-{id:\\d+}")
	public ModelAndView product(@PathVariable String id, String vertical, final HttpServletRequest request, HttpServletResponse response) throws IOException {

		// Getting the product name
		String path= URLEncoder.encode(request.getServletPath().substring(1));



		
		// Retrieve the Product
		Product data;
		try {
			data = esDao.getById(id);

		} catch (ResourceNotFoundException e) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Produit " + request.getServletPath() + " introuvable !");
		}


		// TODO(gof) : Handling redirection if on a vertical match


		if (null == data) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Produit " + request.getServletPath() + " introuvable !");
		}


		// Sending 301 id no match with product name

		if (null == vertical) {

			if (!path.equals(data.getNames().getName())) {
				response.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
				response.setHeader("Location", config.getBaseUrl(Locale.FRANCE) + data.getNames().getName());
				return null;
			}
		} else {
			if (!path.equals(vertical+ "%2F" + data.getNames().getName())) {
				response.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
				response.setHeader("Location", config.getBaseUrl(Locale.FRANCE) + vertical+"/"+data.getNames().getName());
				return null;
			}
		}





		//TODO : in a service
		// Adding the affiliationTokens in all prices
		for (AggregatedPrice price : data.getPrice().getOffers()) {
			inferAffiliationToken(request, data, price);
		}

		// Adding the affiliationTokens in min and max price
		//TODO(gof) : could be in price aggregation
		inferAffiliationToken(request, data, data.getPrice().getMinPrice());
		//		inferAffiliationToken(data, data.getPrice().getMaxPrice());

		ModelAndView mv = null;


		mv = defaultModelAndView("product", request);



		mv.addObject("product", data);

		// Adding the diplay country
		if (null != data.getGtinInfos().getCountry()) {
			mv.addObject("originCountry", new ULocale("",data.getGtinInfos().getCountry()).getDisplayCountry( new ULocale(request.getLocale().toString())));
		}

		
		// Adding the brand informations
		mv.addObject("hasBrandLogo", brandService.hasLogo(data.brand()));
		
		// Adding the images resource

		return mv;
	}


	@PostMapping("/*-{id:\\d+}")
	public ModelAndView updateProduct(@RequestParam String productTitle, @RequestParam String productDescription,  @PathVariable String id, final HttpServletRequest request, HttpServletResponse response) throws IOException {

		// Retrieve the Product
		Product data;
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

		esDao.index(data, ProductRepository.MAIN_INDEX_NAME);

		return product(id, null,request, response);

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