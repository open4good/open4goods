package org.open4goods.ui.controllers.webextention;

import java.io.IOException;
import java.util.AbstractMap.SimpleEntry;
import java.util.List;
import java.util.Map;

import org.open4goods.commons.config.yml.ui.VerticalConfig;
import org.open4goods.commons.dao.ProductRepository;
import org.open4goods.commons.exceptions.ResourceNotFoundException;
import org.open4goods.commons.model.BarcodeType;
import org.open4goods.commons.model.product.Product;
import org.open4goods.commons.services.BarcodeValidationService;
import org.open4goods.commons.services.VerticalsConfigService;
import org.open4goods.ui.controllers.ui.UiService;
import org.open4goods.ui.helper.UiHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.json.MappingJackson2JsonView;

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
public class WebExtensionController {

	public static final String WEBEXTENSION_ENDPOINT = "/webextension";
	public static final String WEBEXTENSION_EXISTS_ENDPOINT = "/webextension/exists";

	private static final Logger LOGGER = LoggerFactory.getLogger(WebExtensionController.class);

	private @Autowired ProductRepository productRepository;
	private @Autowired VerticalsConfigService verticalConfigService;
	private @Autowired UiService uiService;
	private @Autowired BarcodeValidationService barcodeValidationService;

	/**
	 * Returns if a product exists
	 * 
	 * @param gtin
	 * @param title
	 * @param request
	 * @param response
	 * @return
	 * @throws IOException
	 */
	@GetMapping(produces = MediaType.APPLICATION_JSON_VALUE, path = WEBEXTENSION_EXISTS_ENDPOINT)
	public ModelAndView webExtensionProduct(@RequestParam(required = false) String gtin, @RequestParam(required = false) String title, final HttpServletRequest request, HttpServletResponse response) throws IOException {

		ModelAndView mv = new ModelAndView(new MappingJackson2JsonView());

		// Checking through GTIN
		if (null != gtin) {

			SimpleEntry<BarcodeType, String> bCode = barcodeValidationService.sanitize(gtin.toString());

			if (bCode.getKey().equals(BarcodeType.UNKNOWN)) {
				mv.addObject("exists", false);
			} else {
				Product data = null;
				try {
					data = productRepository.getById(bCode.getValue());
				} catch (ResourceNotFoundException e) {
					mv.addObject("exists", false);
				}
				if (null != data) {
					mv.addObject("exists", true);
				} else {
					mv.addObject("exists", false);
				}
			}

		} else {
			// Lookup by title

			List<Product> data = productRepository.getByTitle(title);
			if (data.size() == 0) {
				mv.addObject("exists", false);
			} else {
				mv.addObject("exists", true);
			}
		}
		
		return mv;
	}

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

	@GetMapping(WEBEXTENSION_ENDPOINT)
	public ModelAndView webExtensionProduct(@RequestParam(required = false) Long gtin, @RequestParam(required = false) String title, final HttpServletRequest request, HttpServletResponse response) throws IOException {

		ModelAndView mv = null;

		mv = uiService.defaultModelAndView("webextension/product-single", request);

		// Checking through GTIN
		if (null != gtin) {

			SimpleEntry<BarcodeType, String> bCode = barcodeValidationService.sanitize(gtin.toString());

			if (bCode.getKey().equals(BarcodeType.UNKNOWN)) {
				mv.setViewName("/webextension/product-notfound");
//				throw new ResponseStatusException(HttpStatus.BAD_REQUEST,	"Invalid GTIN Format");
			} else {
				Product data;
				try {
					data = productRepository.getById(bCode.getValue());
				} catch (ResourceNotFoundException e) {
					mv.setViewName("webextension/product-notfound");
					throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No product found ");
				}
				if (null != data) {
					mv.addObject("product", data);
				} else {
					mv.setViewName("webextension/product-notfound");
					throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No product found ");
				}
			}

		} else {
			// Lookup by title

			List<Product> data = productRepository.getByTitle(title);
			if (data.size() == 0) {
				mv.setViewName("webextension/product-notfound");
				throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No product found ");
			} else if (data.size() == 1) {
				mv.addObject("product", data.get(0));
			} else {
				mv.setViewName("webextension/product-multiple");
				mv.addObject("products", data);
			}
		}

		Product p = (Product) mv.getModel().get("product");

		if (null != p) {
			VerticalConfig verticalConfig = verticalConfigService.getConfigByIdOrDefault(p.getVertical());
			mv.addObject("verticalConfig", verticalConfig);
			UiHelper uiHelper = new UiHelper(request, verticalConfig, p);
			// Adding the UiHelper class
			mv.addObject("helper", uiHelper);
		}

		return mv;
	}

	@PostMapping(WEBEXTENSION_ENDPOINT)
	// TODO : Remove when no more use by ext
	public ModelAndView webExtensionProduct(@RequestParam(required = false) String dom, final HttpServletRequest request, HttpServletResponse response) throws IOException {
		return webExtensionProduct(9338716005387L, "", request, response);

	}

}