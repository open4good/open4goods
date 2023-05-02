package org.open4goods.ui.controllers.ui;



import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.Locale;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.open4goods.config.yml.ui.VerticalConfig;
import org.open4goods.dao.AggregatedDataRepository;
import org.open4goods.exceptions.InvalidParameterException;
import org.open4goods.exceptions.ResourceNotFoundException;
import org.open4goods.exceptions.TechnicalException;
import org.open4goods.exceptions.ValidationException;
import org.open4goods.model.constants.ResourceTagDictionary;
import org.open4goods.model.data.Resource;
import org.open4goods.model.product.AggregatedData;
import org.open4goods.services.DataSourceConfigService;
import org.open4goods.services.TagCloudService;
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

	private static final String TAGCLOUD_IMG = "tagcloud.png";

	// The siteConfig
	private @Autowired ImageService imageService;
	private @Autowired AggregatedDataRepository esDao;
	private @Autowired GtinService gtinService;
	private @Autowired TagCloudService tagcloudService;
	private @Autowired UiConfig config;
	private @Autowired DataSourceConfigService dsConfigService;
	private @Autowired VerticalsConfigService verticalConfigService;
	
	//////////////////////////////////////////////////////////////
	// Mappings
	//////////////////////////////////////////////////////////////

	/**
	 * The Home page.
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

	
	@GetMapping("/{vertical}/*-{id:\\d+}/"+PNG_IMG)
	public void image(@PathVariable String vertical, @PathVariable String id, final HttpServletResponse response, HttpServletRequest request) throws FileNotFoundException, IOException, ValidationException, TechnicalException {
		VerticalConfig language = verticalConfigService.getLanguageForVerticalPath(vertical);
		
		if (null == language) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Image " + request.getServletPath() + " introuvable !");
		}
		
		image(id, response, request);
		
	}
	
	@GetMapping("/*-{id:\\d+}/"+PNG_IMG)
	public void image(@PathVariable String id, final HttpServletResponse response, HttpServletRequest request) throws FileNotFoundException, IOException, ValidationException, TechnicalException {
		
		// Retrieve the AggregatedData		
		AggregatedData data;
		try {
			data = esDao.getById(id);
		} catch (ResourceNotFoundException e) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "image introuvable !");
		}
		
		// Handling 404
		if (null == data) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "image introuvable !");
		}
		
		// Sending 301 id no match with product name
		String path= URLEncoder.encode(request.getServletPath().substring(1, request.getServletPath().lastIndexOf("/")));								
		if (!path.equals(data.getNames().getName())) {
			response.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
			response.setHeader("Location", config.getBaseUrl(Locale.FRANCE) + data.getNames().getName()+"/"+PNG_IMG);
			return ;
		}
		
				
		//TODO (gof) : not sure pageSize have image, could have any resource
		// Retrieve one of the cover images
		Optional<Resource> img = data.getResources().stream().filter(r -> r.getTags().contains(ResourceTagDictionary.CSV)).findAny();
		
		// If no cover
		if (img.isEmpty()) {			
			img = data.getResources().stream().findAny();
		}
		
		if (img.isPresent()) {
			response.addHeader("Content-type","image/png");
			response.addHeader("Cache-Control","public, max-age="+AppConfig.CACHE_PERIOD_SECONDS);
			
			InputStream stream = imageService.getCoverPng(img.get());
			IOUtils.copy(stream ,response.getOutputStream());
			IOUtils.closeQuietly(stream);			
		} else {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "image introuvable !");
		}
	}

	@GetMapping("/*-{id:\\d+}/"+GTIN_IMG)
	public void gtin(@PathVariable String id, final HttpServletResponse response, HttpServletRequest request) throws FileNotFoundException, IOException, ValidationException, TechnicalException {
		
		// Retrieve the AggregatedData		
		AggregatedData data;
		
		
		try {
			data = esDao.getById(id);
		} catch (ResourceNotFoundException e) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "image introuvable !");
		}
		
		// Handling 404
		if (null == data) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "image introuvable !");
		}
		
		// Sending 301 id no match with product name
		String path= URLEncoder.encode(request.getServletPath().substring(1, request.getServletPath().lastIndexOf("/")));														
		if (!path.equals(data.getNames().getName())) {
			response.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
			response.setHeader("Location", config.getBaseUrl(Locale.FRANCE) + data.getNames().getName()+"/"+GTIN_IMG);
			return ;
		}
		
		response.addHeader("Content-type","image/png");
					response.addHeader("Cache-Control","public, max-age="+AppConfig.CACHE_PERIOD_SECONDS);
		
		InputStream stream = gtinService.gtin(data.gtin());
		IOUtils.copy(stream ,response.getOutputStream());
		IOUtils.closeQuietly(stream);
	}


	@GetMapping("/*-{id:\\d+}/"+TAGCLOUD_IMG)
	public void tagcloud(@PathVariable String id, final HttpServletResponse response,HttpServletRequest request) throws FileNotFoundException, IOException, ValidationException, TechnicalException {
		
		// Retrieve the AggregatedData		
		AggregatedData data;
		try {
			data = esDao.getById(id);
		} catch (ResourceNotFoundException e) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "image introuvable !");
		}
		
		// Handling 404
		if (null == data) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "image introuvable !");
		}
		
		// Sending 301 id no match with product name
		String path= URLEncoder.encode(request.getServletPath().substring(1, request.getServletPath().lastIndexOf("/")));													
		if (!path.equals(data.getNames().getName())) {
			response.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
			response.setHeader("Location", config.getBaseUrl(Locale.FRANCE) + data.getNames().getName()+"/"+TAGCLOUD_IMG);
			return ;
		}
		
		response.addHeader("Content-type","image/png");
					response.addHeader("Cache-Control","public, max-age="+AppConfig.CACHE_PERIOD_SECONDS);
		
		InputStream stream = tagcloudService.getImageStream(data);
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

	
}