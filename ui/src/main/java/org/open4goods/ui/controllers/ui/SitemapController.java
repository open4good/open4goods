package org.open4goods.ui.controllers.ui;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.open4goods.ui.config.yml.UiConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

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
public class SitemapController  {

	private static final Logger LOGGER = LoggerFactory.getLogger(SitemapController.class);
	private @Autowired UiService uiService;


	// The siteConfig
	private final UiConfig config;

	public SitemapController(UiConfig config, UiService uiService) {
		this.config = config;
		this.uiService = uiService;
	}

	//////////////////////////////////////////////////////////////
	// Mappings
	//////////////////////////////////////////////////////////////

	/**
	 * The Home page.
	 *
	 * @param request
	 * @param response
	 * @return
	 * @throws UnirestException
	 */




	@GetMapping(path= "/sitemap/{sitemap}", produces = MediaType.APPLICATION_XML_VALUE)
	public void sitemapFile(@PathVariable String sitemap, final HttpServletRequest request, HttpServletResponse response) throws IOException {

		String siteLanguage =  uiService.getSiteLanguage(request);

		String siteMapFolder = config.siteMapFolder() + "/"+siteLanguage + "/";
		// Checking a real existing file
		boolean existingfile = Files.list(Path.of(siteMapFolder)).anyMatch(e->e.toFile().getName().equals(sitemap));
		if (!existingfile) {
			response.sendError(404);
			return;
		}

		File sitemapFile = new File(siteMapFolder+sitemap);
		response.setContentType(MediaType.APPLICATION_XML_VALUE);

		try (InputStream stream = new BufferedInputStream(FileUtils.openInputStream(sitemapFile))) {
			IOUtils.copy(stream, response.getOutputStream());
		}
	}
}