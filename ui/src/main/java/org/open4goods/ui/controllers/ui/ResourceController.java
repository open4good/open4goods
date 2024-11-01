package org.open4goods.ui.controllers.ui;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URLConnection;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.open4goods.commons.exceptions.InvalidParameterException;
import org.open4goods.commons.exceptions.TechnicalException;
import org.open4goods.commons.exceptions.ValidationException;
import org.open4goods.commons.model.data.Resource;
import org.open4goods.commons.services.BrandService;
import org.open4goods.commons.services.DataSourceConfigService;
import org.open4goods.commons.services.ImageGenerationService;
import org.open4goods.commons.services.ResourceService;
import org.open4goods.ui.config.AppConfig;
import org.open4goods.ui.config.yml.UiConfig;
import org.open4goods.ui.services.GtinService;
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
 * TODO : "fast" Caching (eg : products are call 7/8 times per pages)
 */
public class ResourceController {

	private static final Logger LOGGER = LoggerFactory.getLogger(ResourceController.class);

	private static final String CONTENT_TYPE_IMAGE_PNG = "image/png";
	private static final String CONTENT_TYPE_JAVA_ARCHIVE = "application/java-archive";
	private static final String HEADER_CONTENT_TYPE = "Content-type";
	private static final String HEADER_CACHE_CONTROL = "Cache-Control";

	
	private final ImageGenerationService imageGenerationService;
	private final UiConfig config;
	private final DataSourceConfigService datasourceConfigService;
	private final BrandService brandService;
	private final ResourceService resourceService;
	private final GtinService gtinService;

	public ResourceController( UiConfig config, DataSourceConfigService dsConfigService, ResourceService resourceService, BrandService brandService, ImageGenerationService imageGenerationService, GtinService gtinService) {
		this.imageGenerationService = imageGenerationService;
		this.config = config;
		this.datasourceConfigService = dsConfigService;
		this.brandService = brandService;
		this.resourceService = resourceService;
		this.gtinService = gtinService;
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
	@GetMapping("/{type:images|videos|pdfs}/{resourceName:[\\w|\\-]+}_{hashkey:\\d+}.{ext:[a-z]{2,4}}")
	public void resource(
			@PathVariable String type, 
	        @PathVariable String hashkey, 
	        @PathVariable String resourceName,
	        @PathVariable String ext,	        
	        final HttpServletResponse response, HttpServletRequest request) {
		
			Resource img = new Resource();
			img.setCacheKey(hashkey);
			img.setMimeType(URLConnection.guessContentTypeFromName("."+ext));
	
		try (InputStream stream = resourceService.getResourceFileStream(img)) {
			setResponseAndCopyStream(response, stream, img.getMimeType());
		} catch (FileNotFoundException e) {
			throw new ResponseStatusException( HttpStatus.NOT_FOUND, "Resource not found"	);
		} catch (IOException e) {
			throw new ResponseStatusException( HttpStatus.INTERNAL_SERVER_ERROR, "Error handling resource"	);
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

		try {
			if (!brandService.hasLogo(brand.toUpperCase())) {
				throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Image introuvable !");
			}

			response.addHeader(HEADER_CONTENT_TYPE, CONTENT_TYPE_IMAGE_PNG);
			response.addHeader(HEADER_CACHE_CONTROL, "public, max-age=" + AppConfig.CACHE_PERIOD_SECONDS);

			try (InputStream stream = brandService.getLogo(brand.toUpperCase())) {
				IOUtils.copy(stream, response.getOutputStream());
			}
		} catch (Exception e) {
			LOGGER.error("Error while rendering brand : {} - {}",brand, e.getMessage());
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
	public void datasourceIcon(@PathVariable String datasourceName, final HttpServletResponse response) {

		response.addHeader(HEADER_CONTENT_TYPE, CONTENT_TYPE_IMAGE_PNG);
		response.addHeader(HEADER_CACHE_CONTROL, "public, max-age=" + AppConfig.CACHE_PERIOD_SECONDS);

		try (InputStream stream = datasourceConfigService.getFavicon(datasourceName)) {
			IOUtils.copy(stream, response.getOutputStream());
		} catch (FileNotFoundException | InvalidParameterException e) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Image introuvable !");
		} catch (IOException e) {
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error rendering image");
		} 
	}

	/**
	 * Serves the gtin image of the specified data source.
	 *
	 * @param datasourceName the name of the data source.
	 * @param response       the HttpServletResponse to write the icon to.
	 * @throws IOException               if an I/O error occurs.
	 * @throws InvalidParameterException if an invalid parameter is provided.
	 */
	@GetMapping("/images/{id:\\d+}-gtin.png")
	// TODO : sec, IP Filter protection
	public void gtinImage(@PathVariable String id, final HttpServletResponse response) {

		response.addHeader(HEADER_CONTENT_TYPE, CONTENT_TYPE_IMAGE_PNG);
		response.addHeader(HEADER_CACHE_CONTROL, "public, max-age=" + AppConfig.CACHE_PERIOD_SECONDS);

		try (InputStream stream = gtinService.gtin(id)) {
			IOUtils.copy(stream, response.getOutputStream());
		} catch (Exception e) {
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error rendering image");
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
		} catch (FileNotFoundException | InvalidParameterException e) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Image introuvable !");
		} catch (IOException e) {
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error rendering image");
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
	 * Serves the generated image for the specified vertical.
	 *
	 * @param verticalId the ID of the vertical.
	 * @param response   the HttpServletResponse to write the image to.
	 * @throws IOException if an I/O error occurs.
	 */
	@GetMapping("/images/verticals/{verticalId}.png")
	public void serveVerticalImage(@PathVariable String verticalId, final HttpServletResponse response) throws IOException {
		String fileName = verticalId + ".png";
		File imageFile = new File(imageGenerationService.getImagesFolder(), fileName);

		if (!imageFile.exists()) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Image not found");
		}

		response.addHeader(HEADER_CONTENT_TYPE, CONTENT_TYPE_IMAGE_PNG);
		response.addHeader(HEADER_CACHE_CONTROL, "public, max-age=" + AppConfig.CACHE_PERIOD_SECONDS);

		try (InputStream stream = new FileInputStream(imageFile)) {
			IOUtils.copy(stream, response.getOutputStream());
		} catch (FileNotFoundException e) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Image not found", e);
		} catch (IOException e) {
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error rendering image", e);
		}
	}

}
