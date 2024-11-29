package org.open4goods.ui.controllers.ui;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.open4goods.commons.config.yml.ui.VerticalConfig;
import org.open4goods.commons.dao.ProductRepository;
import org.open4goods.commons.exceptions.ResourceNotFoundException;
import org.open4goods.commons.model.data.AiDescription;
import org.open4goods.commons.model.data.AiDescriptions;
import org.open4goods.commons.model.data.ContributionVote;
import org.open4goods.commons.model.dto.AttributesFeatureGroups;
import org.open4goods.commons.model.product.AggregatedPrice;
import org.open4goods.commons.model.product.PriceTrend;
import org.open4goods.commons.model.product.Product;
import org.open4goods.commons.services.BarcodeValidationService;
import org.open4goods.commons.services.BrandService;
import org.open4goods.commons.services.IcecatService;
import org.open4goods.commons.services.SerialisationService;
import org.open4goods.commons.services.VerticalsConfigService;
import org.open4goods.ui.config.yml.UiConfig;
import org.open4goods.ui.helper.UiHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
public class ProductController  {

	private static final Logger LOGGER = LoggerFactory.getLogger(ProductController.class);

	private @Autowired UiConfig config;
	
	private @Autowired ProductRepository productRepository;

	private @Autowired SerialisationService serialisationService;

	private @Autowired VerticalsConfigService verticalConfigService;

	private @Autowired BrandService brandService;
	private @Autowired UiService uiService;

	// TODO: Should not have a direct dependency to the icecat service,
	// icecat stuff should be exposed through preloading vertical config 
	private @Autowired 	IcecatService icecatService;
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
	public ModelAndView productInVertical(@PathVariable String vertical, @PathVariable Long id, final HttpServletRequest request, HttpServletResponse response) throws IOException {


		VerticalConfig vConf = verticalConfigService.getVerticalForPath(vertical);

		if (null == vConf) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Produit " + request.getServletPath() + " introuvable !");
		}

		ModelAndView mv = buildProductView(id, vertical,request, response);

		// Force authentication for wip verticals 
		if (vConf.isEnabled() == false && !mv.getModel().containsKey("user")) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Produit " + request.getServletPath() + " inaccessible !");
		}
		
		
		return mv;


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
	
	@GetMapping("/{id:\\d+}*")
	public ModelAndView product(@PathVariable Long id, final HttpServletRequest request, HttpServletResponse response) throws IOException {

		ModelAndView ret = buildProductView(id, null, request, response);;
		
		if (null!= ret.getStatus() && ret.getStatus().is3xxRedirection() ) {
			return ret;
		}
		
		UiHelper uiHelper = (UiHelper) ret.getModel().get("helper");
		String url = uiHelper.url();
	
		// Testing if on a vertical, redirect if so
		Product product = (Product) ret.getModel().get("product");
		
		if (null != product && !StringUtils.isEmpty(product.getVertical())) {
			String vPath = verticalConfigService.getConfigById(product.getVertical()).getBaseUrl(uiService.getSiteLocale(request)); 
			ModelAndView mv = new ModelAndView("redirect:/"  + vPath+ "/"+url);
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
	private ModelAndView buildProductView(Long id, String vertical, final HttpServletRequest request,
			HttpServletResponse response) {
		
		try {
			// Getting the product name
			String path= URLEncoder.encode(request.getServletPath().substring(1), StandardCharsets.UTF_8);
			
			
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


			ModelAndView mv = null;


			
			
			
			mv = uiService.defaultModelAndView("product", request);

			if (mv.getModel().get("user") != null) {
				mv.addObject("raw", serialisationService.toJson(data, true));
			}


			mv.addObject("product", data);
			
			// Fetching better and best objects
			if (null != data.ecoscore()) {
				
				Long globalBestId = data.getRanking().getGlobalBest();
				Long globalBetter = data.getRanking().getGlobalBetter();
				
				if (null != globalBestId && globalBestId.longValue() !=0) {
					Product best = null;
					try {
						best = productRepository.getById(globalBestId);
					} catch (ResourceNotFoundException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					mv.addObject("best", best);
				}
				
				if (null != globalBetter && globalBetter.longValue() != 0) {
					Product better = null;
					try {
						better = productRepository.getById(globalBetter);
					} catch (ResourceNotFoundException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					mv.addObject("better", better);
				}
				
				
			}
			
			
			// Adding the cover image
			
			String cover = "/icons/no-image.png";
			if (data.getCoverImagePath() != null) {
			    cover =  data.getCoverImagePath();
			} else {
				cover = data.externalCover();
			}
			mv.addObject("cover", cover);
			
			// Easiying accessess to pros and cons
			
			List<String> pros = null;
			List<String> cons = null;
			List<String> globalDescriptionParagraphs = null;
			List<String> ecologicalDescriptionParagraphs = null;
			
			AiDescriptions aiDescriptions = data.getGenaiTexts().i18n(request);
			if (null != aiDescriptions) {
				
				AiDescription ps = aiDescriptions.getDescriptions().get("pros");
				if (null != ps) {
					pros = (Arrays.asList(ps.getContent().split("\n|<br/>|;")));
				}
				
				AiDescription cs = data.getGenaiTexts().i18n(request).getDescriptions().get("cons");
				if (null != cs) {
					cons = (Arrays.asList(cs.getContent().split("\n|<br/>|;")));
				}

				AiDescription gd = data.getGenaiTexts().i18n(request).getDescriptions().get("global-description");
				if (gd != null) {
					globalDescriptionParagraphs = Arrays.asList(gd.getContent().split(";"));
				}

				AiDescription ed = data.getGenaiTexts().i18n(request).getDescriptions().get("ecological-description");
				if (ed != null) {
					ecologicalDescriptionParagraphs = Arrays.asList(ed.getContent().split(";"));
				}
			}

			mv.addObject("pros", pros);
			mv.addObject("cons", cons);
			mv.addObject("globalDescriptionParagraphs", globalDescriptionParagraphs);
			mv.addObject("ecologicalDescriptionParagraphs", ecologicalDescriptionParagraphs);
			
			// Building the pricetrend
			
			PriceTrend newTrends = PriceTrend.of(data.getPrice().getNewPricehistory());
			
			mv.addObject("newTrend", newTrends);
			
			
			VerticalConfig verticalConfig = verticalConfigService.getVerticalForPath(vertical);
			
			List<AttributesFeatureGroups> features = icecatService.features(verticalConfig, mv.getModelMap().get("siteLanguage").toString(), data);
			mv.addObject("features",features);
			
			
			mv.addObject("verticalConfig", verticalConfig);

			UiHelper uiHelper = new UiHelper(request, verticalConfig, data);
			// Adding the UiHelper class
			mv.addObject("helper", uiHelper);
			
			if (null == vertical) {

				// Sending 301 id no match with product name
				if (uiHelper.url() != null && !path.equals(uiHelper.url())) {
					response.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
					// TODO : I18n
					mv = new ModelAndView("redirect:/" + uiHelper.url());
					mv.setStatus(HttpStatus.MOVED_PERMANENTLY);
					mv.addObject("product", data);
					return mv;
				}
			} else {
				if (null == data.getVertical()) {
					// TODO : I18n path (fr)
					mv = new ModelAndView(
							"redirect:/" + uiHelper.url());
					mv.setStatus(HttpStatus.MOVED_PERMANENTLY);
					mv.addObject("product", data);
					return mv;
				} else	if (!vertical.equals(verticalConfigService.getPathForVerticalLanguage(mv.getModel().get("siteLanguage").toString(), verticalConfig))) {
					String p = verticalConfigService.getPathForVerticalLanguage(mv.getModel().get("siteLanguage").toString(), verticalConfig);
					mv = new ModelAndView("redirect:/"+ p+"/"+uiHelper.url());
					mv.setStatus(HttpStatus.MOVED_PERMANENTLY);
					mv.addObject("product", data);
					return mv;
				} else if (!path.equals(vertical+ "%2F" + URLEncoder.encode(uiHelper.url(), StandardCharsets.UTF_8))) {
					mv = new ModelAndView("redirect:/"+ vertical+"/"+uiHelper.url());
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
			if (null != data.getPrice().getMinPrice()) {
				inferAffiliationToken(request, data, data.getPrice().getMinPrice());
			//		inferAffiliationToken(data, data.getPrice().getMaxPrice());
			}
			
			
			// Adding the diplay country
			if (null != data.getGtinInfos().getCountry()) {
				mv.addObject("originCountry", new ULocale("",data.getGtinInfos().getCountry()).getDisplayCountry( new ULocale(request.getLocale().toString())));
			}

			
			// Adding the brand informations
			mv.addObject("hasBrandLogo", brandService.hasLogo(data.brand()));
			
			// Adding the images resource

			return mv;
		} catch (Exception e) {
			LOGGER.error("Error while building view for {}",id,e);
			
			throw e;
		}
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
			ContributionVote token = new ContributionVote( price, data);
			
			
			String serToken = URLEncoder.encode(serialisationService.compressString(serialisationService.toJson(token)), Charset.defaultCharset());
			price.setAffiliationToken(serToken);
		} catch (Exception e) {
			LOGGER.error("Error while generating affiliation token for {} : {}", data, e.getMessage());
		}
	}

}