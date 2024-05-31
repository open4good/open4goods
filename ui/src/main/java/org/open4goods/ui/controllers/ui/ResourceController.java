package org.open4goods.ui.controllers.ui;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.function.Predicate;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.open4goods.dao.ProductRepository;
import org.open4goods.exceptions.InvalidParameterException;
import org.open4goods.exceptions.ResourceNotFoundException;
import org.open4goods.exceptions.TechnicalException;
import org.open4goods.exceptions.ValidationException;
import org.open4goods.model.data.Resource;
import org.open4goods.model.product.Product;
import org.open4goods.services.BrandService;
import org.open4goods.services.DataSourceConfigService;
import org.open4goods.services.ResourceService;
import org.open4goods.services.VerticalsConfigService;
import org.open4goods.ui.config.AppConfig;
import org.open4goods.ui.config.yml.UiConfig;
import org.open4goods.ui.services.GtinService;
import org.open4goods.ui.services.ImageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Controller
/**
 * This controller maps Resources to web endpoints.
 */
public class ResourceController {

	private static final Logger LOGGER = LoggerFactory.getLogger(ResourceController.class);

	private static final String CONTENT_TYPE_IMAGE_PNG = "image/png";
	private static final String CONTENT_TYPE_JAVA_ARCHIVE = "application/java-archive";
	private static final String HEADER_CONTENT_TYPE = "Content-type";
	private static final String HEADER_CACHE_CONTROL = "Cache-Control";

	private final ImageService imageService;
	private final ProductRepository esDao;
	private final GtinService gtinService;
	private final UiConfig config;
	private final DataSourceConfigService datasourceConfigService;
	private final VerticalsConfigService verticalConfigService;
	private final BrandService brandService;
	private final ResourceService resourceService;

	public ResourceController(ImageService imageService, ProductRepository esDao, GtinService gtinService, UiConfig config, DataSourceConfigService dsConfigService, ResourceService resourceService, VerticalsConfigService verticalConfigService, BrandService brandService) {
		this.imageService = imageService;
		this.esDao = esDao;
		this.gtinService = gtinService;
		this.config = config;
		this.datasourceConfigService = dsConfigService;
		this.verticalConfigService = verticalConfigService;
		this.brandService = brandService;
		this.resourceService = resourceService;
	}



	/**
	 * Serves the resource specified by type, GTIN, hash key, and resource name.
	 *
	 * @param type         the type of the resource.
	 * @param gtin         the GTIN of the resource.
	 * @param hashKey      the hash key of the resource.
	 * @param resourceName the name of the resource.
	 * @param response     the HttpServletResponse to write the resource to.
	 * @param request      the HttpServletRequest.
	 * @throws IOException if an I/O error occurs.
	 */
	@GetMapping("/{type}/{gtin:\\d+}-{hashkey:\\d+}-{resourceName}")
	public void resource(@PathVariable String type, @PathVariable String gtin, @PathVariable String hashKey, @PathVariable String resourceName, final HttpServletResponse response, HttpServletRequest request)	throws IOException {

		Product data = handleResourceNotFoundException(gtin);
		Resource img = retrieveResource(data, e -> e.getCacheKey().equals(hashKey));

		if (img != null) {
			try (InputStream stream = resourceService.getResourceFileStream(img)) {
				setResponseAndCopyStream(response, stream, img.getMimeType());
			}
		} else {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found!");
		}
	}

	/**
	 * Serves the logo image of the specified brand.
	 *
	 * @param brand    the brand name.
	 * @param response the HttpServletResponse to write the image to.
	 * @throws IOException               if an I/O error occurs.
	 * @throws ValidationException       if validation fails.
	 * @throws TechnicalException        if a technical error occurs.
	 * @throws InvalidParameterException if an invalid parameter is provided.
	 */
	@GetMapping("/images/marques/{brand}.png")
	public void brandLogo(@PathVariable String brand, final HttpServletResponse response) throws IOException, ValidationException, TechnicalException, InvalidParameterException {

		if (!brandService.hasLogo(brand.toUpperCase())) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Image introuvable !");
		}

		response.addHeader(HEADER_CONTENT_TYPE, CONTENT_TYPE_IMAGE_PNG);
		response.addHeader(HEADER_CACHE_CONTROL, "public, max-age=" + AppConfig.CACHE_PERIOD_SECONDS);

		try (InputStream stream = brandService.getLogo(brand.toUpperCase())) {
			IOUtils.copy(stream, response.getOutputStream());
		}
	}
	
	/**
	 * Serves the icon image of the specified data source.
	 *
	 * @param datasourceName the name of the data source.
	 * @param response       the HttpServletResponse to write the icon to.
	 * @throws IOException               if an I/O error occurs.
	 * @throws InvalidParameterException if an invalid parameter is provided.
	 */
	@GetMapping("/icon/{datasourceName}")
	public void datasourceIcon(@PathVariable String datasourceName, final HttpServletResponse response)	throws IOException, InvalidParameterException {

		response.addHeader(HEADER_CONTENT_TYPE, CONTENT_TYPE_IMAGE_PNG);
		response.addHeader(HEADER_CACHE_CONTROL, "public, max-age=" + AppConfig.CACHE_PERIOD_SECONDS);

		try (InputStream stream = datasourceConfigService.getFavicon(datasourceName)) {
			IOUtils.copy(stream, response.getOutputStream());
		}
	}

	/**
	 * Serves the logo image of the specified data source.
	 *
	 * @param datasourceName the name of the data source.
	 * @param response       the HttpServletResponse to write the logo to.
	 * @throws IOException               if an I/O error occurs.
	 * @throws InvalidParameterException if an invalid parameter is provided.
	 */
	@GetMapping("/logo/{datasourceName}")
	public void datasourceLogo(@PathVariable String datasourceName, final HttpServletResponse response)	throws IOException, InvalidParameterException {

		response.addHeader(HEADER_CONTENT_TYPE, CONTENT_TYPE_IMAGE_PNG);
		response.addHeader(HEADER_CACHE_CONTROL, "public, max-age=" + AppConfig.CACHE_PERIOD_SECONDS);

		try (InputStream stream = datasourceConfigService.getLogo(datasourceName)) {
			IOUtils.copy(stream, response.getOutputStream());
		}
	}

	/**
	 * Serves the latest UI jar file.
	 *
	 * @param response the HttpServletResponse to write the jar file to.
	 * @throws IOException               if an I/O error occurs.
	 * @throws InvalidParameterException if an invalid parameter is provided.
	 * @throws URISyntaxException        if the URI syntax is incorrect.
	 */
	@GetMapping("/ui-latest.jar")
	public void uiJarFile(final HttpServletResponse response) throws IOException, InvalidParameterException, URISyntaxException {

		response.addHeader(HEADER_CONTENT_TYPE, CONTENT_TYPE_JAVA_ARCHIVE);
		response.addHeader(HEADER_CACHE_CONTROL, "public, max-age=" + AppConfig.CACHE_PERIOD_SECONDS);

		try (InputStream stream = new BufferedInputStream(FileUtils.openInputStream(config.uiJarFile()))) {
			IOUtils.copy(stream, response.getOutputStream());
		}
	}

	
	//////////////////////////
	// Response helpers
	/////////////////////////
	/**
	 * Sets the response headers and copies the input stream to the response output
	 * stream.
	 *
	 * @param response    the HttpServletResponse to write the data to.
	 * @param stream      the InputStream to read the data from.
	 * @param contentType the content type of the response.
	 * @throws IOException if an I/O error occurs.
	 */
	private void setResponseAndCopyStream(HttpServletResponse response, InputStream stream, String contentType)
			throws IOException {
		response.addHeader(HEADER_CONTENT_TYPE, contentType);
		response.addHeader(HEADER_CACHE_CONTROL, "public, max-age=" + AppConfig.CACHE_PERIOD_SECONDS);
		IOUtils.copy(stream, response.getOutputStream());
	}

	/**
	 * Handles resource not found exception for the given GTIN.
	 *
	 * @param id the GTIN of the resource.
	 * @return the Product corresponding to the given GTIN.
	 * @throws ResponseStatusException if the resource is not found.
	 */
	private Product handleResourceNotFoundException(String id) {
		Product data;
		try {
			data = esDao.getById(id);
		} catch (ResourceNotFoundException e) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found!");
		}

		if (data == null) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found!");
		}

		return data;
	}

	/**
	 * Retrieves the resource from the product that matches the given predicate.
	 *
	 * @param data      the Product containing the resources.
	 * @param predicate the Predicate to match the resource.
	 * @return the Resource that matches the predicate, or null if no match is
	 *         found.
	 */
	private Resource retrieveResource(Product data, Predicate<Resource> predicate) {
		return data.getResources().stream().filter(predicate).findAny().orElse(null);
	}
}
