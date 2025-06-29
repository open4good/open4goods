package org.open4goods.ui.controllers.ui;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.open4goods.commons.model.data.ContributionVote;
import org.open4goods.commons.model.dto.AttributesFeatureGroups;
import org.open4goods.commons.model.dto.NumericRangeFilter;
import org.open4goods.commons.model.dto.VerticalSearchRequest;
import org.open4goods.commons.model.dto.VerticalSearchResponse;
import org.open4goods.commons.services.BrandService;
import org.open4goods.commons.services.IcecatService;
import org.open4goods.commons.services.SearchService;
import org.open4goods.commons.services.VerticalsConfigService;
import org.open4goods.model.ai.AiDescription;
import org.open4goods.model.ai.AiDescriptions;
import org.open4goods.model.ai.AiReview;
import org.open4goods.model.attribute.AttributeType;
import org.open4goods.model.exceptions.ResourceNotFoundException;
import org.open4goods.model.price.AggregatedPrice;
import org.open4goods.model.price.PriceTrend;
import org.open4goods.model.product.AiReviewHolder;
import org.open4goods.model.product.Product;
import org.open4goods.model.product.ProductCondition;
import org.open4goods.model.vertical.AttributeConfig;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.services.captcha.config.HcaptchaProperties;
import org.open4goods.services.productrepository.services.ProductRepository;
import org.open4goods.services.reviewgeneration.service.ReviewGenerationService;
import org.open4goods.services.serialisation.service.SerialisationService;
import org.open4goods.ui.config.yml.FunFactsConfig;
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


	private @Autowired ReviewGenerationService reviewGenerationService;


	private @Autowired SerialisationService serialisationService;

	private @Autowired VerticalsConfigService verticalConfigService;

	private @Autowired SearchService searchService;

	private @Autowired BrandService brandService;
	private @Autowired UiService uiService;

	private @Autowired HcaptchaProperties captchaProps;

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
	 * @throws Exception
	 * @throws UnirestException
	 */


	@GetMapping("/{vertical}/{id:\\d+}-*")
	public ModelAndView productInVertical(@PathVariable String vertical, @PathVariable Long id, final HttpServletRequest request, HttpServletResponse response) throws Exception {


		VerticalConfig vConf = verticalConfigService.getVerticalForPath(vertical);

		if (null == vConf) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Produit " + request.getServletPath() + " introuvable !");
		}

		ModelAndView mv = buildProductView(id, vertical,request, response);


		if (null != mv.getStatus() && mv.getStatus().equals((HttpStatus.MOVED_PERMANENTLY))) {
			return mv;
		}
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
	public ModelAndView product(@PathVariable Long id, final HttpServletRequest request, HttpServletResponse response) throws Exception {

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
	 * @throws Exception
	 */
	private ModelAndView buildProductView(Long id, String vertical, final HttpServletRequest request,
			HttpServletResponse response) throws Exception {

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




			String tplName = StringUtils.isEmpty(data.getVertical()) ? "product-novertical" : "product";
			mv = uiService.defaultModelAndView(tplName, request);





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


			// Fetching products with scores worse or best in a map.

			Set<Long> worsesId = data.realScores().stream().filter(e->e.getLowestScoreId() != null).map(e->e.getLowestScoreId()).collect(Collectors.toSet());
			Set<Long> bestId = data.realScores().stream().filter(e->e.getHighestScoreId() != null).map(e->e.getHighestScoreId()).collect(Collectors.toSet());
			Set<Long> all = new HashSet<>();
			all.addAll(worsesId);
			all.addAll(bestId);



			//TODO(p1,perf) : heavy cache
			Map<String, Product> prds = productRepository.multiGetById(all);

			// Re-building products in a map
			Map<Long, Product> extrems = all.stream()
			    .map(e -> prds.get(e.toString()))
			    .filter(Objects::nonNull)
			    .collect(Collectors.toMap(
			        Product::getId, // or e -> e.getId(), or e -> e.toString()
			        Function.identity(),
			        (a, b) -> a // in case of duplicates, keep the first
			    ));

			mv.addObject("extrems",extrems);

			// Adding bestOccasionOffer and bestNewOffer
			mv.addObject("bestOccasionOffer", data.getPrice().bestOffer(ProductCondition.OCCASION));
			mv.addObject("bestNewOffer", data.getPrice().bestOffer(ProductCondition.NEW));



			// TODO: i18n





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


			// TODO : remove
			mv.addObject("pros", pros);
			mv.addObject("cons", cons);
			mv.addObject("globalDescriptionParagraphs", globalDescriptionParagraphs);
			mv.addObject("ecologicalDescriptionParagraphs", ecologicalDescriptionParagraphs);

			AiReviewHolder aiReview = data.getReviews().get(mv.getModel().get("siteLanguage"));
			mv.addObject("aiReview", aiReview);


			// Building the pricetrend

			PriceTrend newTrends = PriceTrend.of(data.getPrice().getNewPricehistory(), data.getPrice().bestNewOffer());
			PriceTrend occasionTrend = PriceTrend.of(data.getPrice().getOccasionPricehistory(), data.getPrice().bestOccasionOffer());

			mv.addObject("newTrend", newTrends);
			mv.addObject("occasionTrend", occasionTrend);


			VerticalConfig verticalConfig = verticalConfigService.getVerticalForPath(vertical);

			List<AttributesFeatureGroups> features = icecatService.features(verticalConfig, mv.getModelMap().get("siteLanguage").toString(), data);
			mv.addObject("features",features);



			mv.addObject("verticalConfig", verticalConfig);
			if (null != verticalConfig) {
				mv.addObject("verticalPath",verticalConfigService.getPathForVerticalLanguage("fr",verticalConfig));
			}


			// Adding the stats (from a full search aggregation)
			VerticalSearchRequest statsRequest = buildStatRequest(verticalConfig, data);
			// TODO : Check heavy caching

			if (null != verticalConfig) {
				VerticalSearchResponse statsResponse = searchService.verticalSearch(verticalConfig, statsRequest);
				mv.addObject("stats",statsResponse);
			}


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
                        data.getPrice().setOffers(data.getPrice().getOffers().stream()
                                        .map(p -> inferAffiliationToken(request, data, p))
                                        .collect(Collectors.toSet()));


			// Adding the affiliationTokens in min and max price
			if (null != data.getPrice().getMinPrice()) {
                                data.getPrice().setMinPrice(
                                                inferAffiliationToken(request, data, data.getPrice().getMinPrice()));
			//		inferAffiliationToken(data, data.getPrice().getMaxPrice());
			}


			// Adding the diplay country
			if (null != data.getGtinInfos().getCountry()) {
				mv.addObject("originCountry", new ULocale("",data.getGtinInfos().getCountry()).getDisplayCountry( new ULocale(request.getLocale().toString())));
			}


			// Adding the brand informations
			//TODO(p1) : brand logo
//			mv.addObject("hasBrandLogo", brandService.hasLogo(data.brand()));
			mv.addObject("hasBrandLogo", false);

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
        private AggregatedPrice inferAffiliationToken(HttpServletRequest  request, Product data, AggregatedPrice price)  {

		try {
			ContributionVote token = new ContributionVote( price, data);


                        String serToken = URLEncoder.encode(serialisationService.compressString(serialisationService.toJson(token)), Charset.defaultCharset());
                        price.setAffiliationToken(serToken);

                         return price;
                } catch (Exception e) {
                        LOGGER.error("Error while generating affiliation token for {} : {}", data, e.getMessage());
                        return price;
                }
        }


	///////////////////
	///// For review
	////////////////////
	///
	///	TODO(p2,design) -> Out in dedicated controller
	@GetMapping(path = {"/review-request/{id:\\d+}"}   )
	public ModelAndView review(final HttpServletRequest request, @PathVariable(required = false) String vertical, @PathVariable Long id) {
		ModelAndView ret = uiService.defaultModelAndView("review-request", request)
					.addObject("captchaKey", captchaProps.getKey())
					.addObject("gtin",id);

		// TODO : i18n, check not null
		FunFactsConfig funFacts = config.getFunFacts().i18n("fr");
		ret.addObject("funFacts",funFacts.getFacts());

		return ret;

	}


	// TODO(p1, performance) : heavy cache
	public  VerticalSearchRequest buildStatRequest(VerticalConfig config, Product data) {
		VerticalSearchRequest vRequest = new VerticalSearchRequest();


		vRequest.setSortField("scores.ECOSCORE.value");
		vRequest.setSortOrder("desc");

		vRequest.getNumericFilters().add(new NumericRangeFilter("offersCount", 1.0, 10000.0, 1.0, false));
		vRequest.getNumericFilters().add(new NumericRangeFilter("price.minPrice.price", 0.0001, 500000.0, 100.0, false));


		data.realScores().forEach(s -> {
			vRequest.getNumericFilters().add(new NumericRangeFilter("scores."+s.getName()+".value", 0.0001, 500000.0, 1.0, true));

		});


		return vRequest;
	}


}