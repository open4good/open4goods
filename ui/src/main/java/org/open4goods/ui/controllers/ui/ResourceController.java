package org.open4goods.ui.controllers.ui;



import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.Optional;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.open4goods.dao.ProductRepository;
import org.open4goods.exceptions.InvalidParameterException;
import org.open4goods.exceptions.ResourceNotFoundException;
import org.open4goods.exceptions.TechnicalException;
import org.open4goods.exceptions.ValidationException;
import org.open4goods.model.constants.ResourceTagDictionary;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

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
public class ResourceController extends AbstractUiController {

	private static final Logger LOGGER = LoggerFactory.getLogger(ResourceController.class);

	private static final String PNG_IMG = "image.png";
	private static final String GTIN_IMG = "gtin.png";

	// The siteConfig
	private final ImageService imageService;
	private final ProductRepository esDao;
	private final GtinService gtinService;
	private final UiConfig config;
	private final DataSourceConfigService dsConfigService;
	private final VerticalsConfigService verticalConfigService;
	private final BrandService brandService;
	private final ResourceService resourceService;
	

	public ResourceController(ImageService imageService, ProductRepository esDao, GtinService gtinService, UiConfig config, DataSourceConfigService dsConfigService,  ResourceService resourceService, VerticalsConfigService verticalConfigService, BrandService brandService) {
		this.imageService = imageService;
		this.esDao = esDao;
		this.gtinService = gtinService;
		this.config = config;
		this.dsConfigService = dsConfigService;
		this.verticalConfigService = verticalConfigService;
		this.brandService = brandService;
		this.resourceService = resourceService;
	}

	//////////////////////////////////////////////////////////////
	// Mappings
	//////////////////////////////////////////////////////////////

	
	/**
	 * The brand logos images.
	 *
	 * @param request
	 * @param response
	 * @return
	 * @throws TechnicalException
	 * @throws ValidationException
	 * @throws IOException
	 * @throws FileNotFoundException
	 * @throws InvalidParameterException 
	 * @throws UnirestException
	 */


	@GetMapping("/images/marques/{brand}.png")
	public void brandLogo(@PathVariable String brand, final HttpServletResponse response, HttpServletRequest request) throws FileNotFoundException, IOException, ValidationException, TechnicalException, InvalidParameterException {		

		if (!brandService.hasLogo(brand.toUpperCase())) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Image " + request.getServletPath() + " introuvable !");
		}
		
		response.addHeader("Content-type","image/png");
		response.addHeader("Cache-Control","public, max-age="+AppConfig.CACHE_PERIOD_SECONDS);

		InputStream stream = brandService.getLogo(brand.toUpperCase());
		IOUtils.copy(stream ,response.getOutputStream());
		IOUtils.closeQuietly(stream);
		
		
	}
	
	
	
	/**
	 * The vertical Home page.
	 *
	 * @param request
	 * @param response
	 * @return
	 * @throws TechnicalException
	 * @throws ValidationException
	 * @throws IOException
	 * @throws FileNotFoundException
	 * @throws UnirestException
	 */


//	@GetMapping("/{vertical}/*-{id:\\d+}/"+PNG_IMG)
//	public void image(@PathVariable String vertical, @PathVariable String id, final HttpServletResponse response, HttpServletRequest request) throws FileNotFoundException, IOException, ValidationException, TechnicalException {
//		VerticalConfig language = verticalConfigService.getVerticalForPath(vertical);
//
//		if (null == language) {
//			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Image " + request.getServletPath() + " introuvable !");
//		}
//
//		image(id, response, request);
//
//	}

	@GetMapping("/images/{gtin:\\d+}-cover.png")
	public void image(@PathVariable String gtin, final HttpServletResponse response, HttpServletRequest request) throws IOException  {

		
		
		
		
		 image(gtin, 0, response, request);
	}
	@GetMapping("/images/{gtin:\\d+}-{imgNumber:\\d+}.png")
	public void image(@PathVariable String gtin, @PathVariable int imgNumber, final HttpServletResponse response, HttpServletRequest request) throws IOException  {

		// Retrieve the Product
		Product data;
		try {
			data = esDao.getById(gtin);
		} catch (ResourceNotFoundException e) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "image introuvable !");
		}

		// Handling 404
		if (null == data) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "image introuvable !");
		}


		//TODO (gof) : not sure pageSize have image, could have any resource
		// Retrieve one of the cover images
		Resource img = null;
		
		if (imgNumber <   data.getImages().size()) {
			img = data.getImages().get(imgNumber);
		} 


		if (null != img) {
			// TODO : Should be webp
			response.addHeader("Content-type","image/png");
			response.addHeader("Cache-Control","public, max-age="+AppConfig.CACHE_PERIOD_SECONDS);
			InputStream stream = resourceService.getFileStream(img);
			IOUtils.copy(stream ,response.getOutputStream());
			IOUtils.closeQuietly(stream);
		} else {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "image introuvable !");
		}
	}

	
	@GetMapping("/images/{id:\\d+}-gtin.png")
	public void gtin(@PathVariable String id, final HttpServletResponse response, HttpServletRequest request) throws FileNotFoundException, IOException, ValidationException, TechnicalException {

		// Retrieve the Product
		Product data;

		try {
			data = esDao.getById(id);
		} catch (ResourceNotFoundException e) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "image introuvable !");
		}

		// Handling 404
		if (null == data) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "image introuvable !");
		}

		response.addHeader("Content-type","image/png");
		response.addHeader("Cache-Control","public, max-age="+AppConfig.CACHE_PERIOD_SECONDS);

		InputStream stream = gtinService.gtin(data.gtin());
		IOUtils.copy(stream ,response.getOutputStream());
		IOUtils.closeQuietly(stream);
	}





	@GetMapping("/icon/{datasourceName}")
	public void datasourceIcon(@PathVariable String datasourceName, final HttpServletResponse response) throws FileNotFoundException, IOException, InvalidParameterException  {
		response.addHeader("Content-type","image/png");
		response.addHeader("Cache-Control","public, max-age="+AppConfig.CACHE_PERIOD_SECONDS);

		InputStream stream = dsConfigService.getFavicon(datasourceName);
		IOUtils.copy(stream ,response.getOutputStream());
		IOUtils.closeQuietly(stream);
	}

	@GetMapping("/logo/{datasourceName}")
	public void datasourceLogo(@PathVariable String datasourceName, final HttpServletResponse response) throws FileNotFoundException, IOException, InvalidParameterException  {
		response.addHeader("Content-type","image/png");
		response.addHeader("Cache-Control","public, max-age="+AppConfig.CACHE_PERIOD_SECONDS);
		InputStream stream = dsConfigService.getLogo(datasourceName);
		IOUtils.copy(stream ,response.getOutputStream());
		IOUtils.closeQuietly(stream);
	}

	
	@GetMapping("/ui-latest.jar")
	public void uiJarFile( final HttpServletResponse response) throws FileNotFoundException, IOException, InvalidParameterException, URISyntaxException  {
		
		response.addHeader("Content-type","application/java-archive");
		response.addHeader("Cache-Control","public, max-age="+AppConfig.CACHE_PERIOD_SECONDS);
		InputStream stream = new BufferedInputStream(FileUtils.openInputStream(config.uiJarFile()));
		IOUtils.copy(stream ,response.getOutputStream());
		IOUtils.closeQuietly(stream);
	}


}