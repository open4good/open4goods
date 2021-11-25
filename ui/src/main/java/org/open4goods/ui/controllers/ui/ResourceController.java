package org.open4goods.ui.controllers.ui;



import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.Locale;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.open4goods.dao.AggregatedDataRepository;
import org.open4goods.exceptions.TechnicalException;
import org.open4goods.exceptions.ValidationException;
import org.open4goods.model.constants.ResourceTagDictionary;
import org.open4goods.model.data.Resource;
import org.open4goods.model.product.AggregatedData;
import org.open4goods.services.TagCloudService;
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

	@GetMapping("/*-{id:\\d+}/"+PNG_IMG)
	public void image(@PathVariable String id, final HttpServletResponse response, HttpServletRequest request) throws FileNotFoundException, IOException, ValidationException, TechnicalException {
		
		// Retrieve the AggregatedData		
		AggregatedData data = esDao.getById(id);
		
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
		
				
		//TODO (gof) : not sure to have image, could have any resource
		// Retrieve one of the cover images
		Optional<Resource> img = data.getResources().stream().filter(r -> r.getTags().contains(ResourceTagDictionary.CSV)).findAny();
		
		// If no cover
		if (img.isEmpty()) {			
			img = data.getResources().stream().findAny();
		}
		
		if (img.isPresent()) {
			response.addHeader("Content-type","image/png");
			IOUtils.copy(imageService.getCoverPng(img.get()) ,response.getOutputStream());
		}	
	}

	@GetMapping("/*-{id:\\d+}/"+GTIN_IMG)
	public void gtin(@PathVariable String id, final HttpServletResponse response, HttpServletRequest request) throws FileNotFoundException, IOException, ValidationException, TechnicalException {
		
		// Retrieve the AggregatedData		
		AggregatedData data = esDao.getById(id);
		
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
		IOUtils.copy(gtinService.gtin(data.gtin()) ,response.getOutputStream());
	}


	@GetMapping("/*-{id:\\d+}/"+TAGCLOUD_IMG)
	public void tagcloud(@PathVariable String id, final HttpServletResponse response,HttpServletRequest request) throws FileNotFoundException, IOException, ValidationException, TechnicalException {
		
		// Retrieve the AggregatedData		
		AggregatedData data = esDao.getById(id);
		
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
		IOUtils.copy(tagcloudService.getImageStream(data) ,response.getOutputStream());
	}
	
}