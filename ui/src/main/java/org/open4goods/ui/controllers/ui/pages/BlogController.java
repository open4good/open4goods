package org.open4goods.ui.controllers.ui.pages;

import java.io.IOException;
import java.util.List;

import org.open4goods.dao.ProductRepository;
import org.open4goods.model.blog.BlogPost;
import org.open4goods.services.DataSourceConfigService;
import org.open4goods.services.VerticalsConfigService;
import org.open4goods.ui.controllers.ui.UiService;
import org.open4goods.ui.services.BlogService;
import org.open4goods.xwiki.services.XWikiHtmlService;
import org.open4goods.xwiki.services.XwikiFacadeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import com.rometools.rome.io.FeedException;

import cz.jiripinkas.jsitemapgenerator.ChangeFreq;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Controller
public class BlogController  implements SitemapExposedController{

	public static final String DEFAULT_PATH="/blog";

	private static final Logger LOGGER = LoggerFactory.getLogger(BlogController.class);

	private final ProductRepository aggregatedDataRepository;
	private final DataSourceConfigService datasourceConfigService;
	private final VerticalsConfigService verticalConfigService;
	private final XwikiFacadeService xwikiFacadeService;
	private BlogService blogService;
	private @Autowired UiService uiService;
	
	public BlogController(ProductRepository aggregatedDataRepository, DataSourceConfigService datasourceConfigService, VerticalsConfigService verticalConfigService, BlogService blogService, XwikiFacadeService xwikiFacadeService) {
		this.aggregatedDataRepository = aggregatedDataRepository;
		this.datasourceConfigService = datasourceConfigService;
		this.verticalConfigService = verticalConfigService;
		this.blogService = blogService;
		this.xwikiFacadeService = xwikiFacadeService;
	}


	@Override
	public SitemapEntry getExposedUrls() {
		return SitemapEntry.of(SitemapEntry.LANGUAGE_DEFAULT, DEFAULT_PATH, 0.5, ChangeFreq.WEEKLY);
				
	}
	
	@GetMapping(DEFAULT_PATH)
	public ModelAndView blogIndex(final HttpServletRequest request, @RequestParam(required = false) String tag) {
		ModelAndView model = uiService.defaultModelAndView("blog", request);
		model.addObject("totalItems", aggregatedDataRepository.countMainIndex());
		model.addObject("url",  "/");
		List<BlogPost> posts = blogService.getPosts();
		
		// Filtering by tag
		if (null != tag) {
			posts = posts.stream().filter(e->e.getCategory().contains(tag)).toList();
		}
		
		
		model.addObject("posts", posts);		
		return model;
	}


	@GetMapping(value=DEFAULT_PATH+"/rss",  produces = "application/xml")
	public void rss(HttpServletResponse response, HttpServletRequest request ) throws FeedException, IOException {
		response.setContentType("application/rss+xml");
		response.setCharacterEncoding("UTF-8");
		response.setHeader("Cache-Control", "no-cache");
		response.setHeader("Pragma", "no-cache");
		response.setDateHeader("Expires", -1);
		response.getWriter().write(blogService.rss(request.getLocale().getLanguage()));		
	}
	
	
	@GetMapping(DEFAULT_PATH + "/{post}")
	public ModelAndView post(@PathVariable String post, final HttpServletRequest request) {
		ModelAndView model = uiService.defaultModelAndView("blog-post", request);

				BlogPost blogPost = blogService.getPostsByUrl().get(post);
		
		if (null == blogPost) {
			LOGGER.error("Blog post not found : {}", post);
			// TODO : Throw a 404
			return new ModelAndView("redirect:/blog");
		}
		model.addObject("post", blogPost);		
		
//		model.addObject("pages", pages);
		return model;
	}

	@GetMapping(DEFAULT_PATH + "/{page}/{filename}")	
	// TODO : Caching
	public void attachment( @PathVariable(name = "page") String page, @PathVariable(name = "filename") String filename, final HttpServletRequest request, HttpServletResponse response) throws IOException  {
		// TODO : Blog
		byte[] bytes = xwikiFacadeService.downloadAttachment("Blog", page, filename);
		response.setContentType(xwikiFacadeService.detectMimeType(filename));
		// TODO : Have a streamed version
		response.getOutputStream().write(bytes);
	}
	
	
	@GetMapping(XWikiHtmlService.PROXYFIED_FOLDER+ "/**")	
	// TODO : Caching
	// TODO : Mutualize with the one in blog controller (?)
	// TODO : Serve here the classical xwiki download content, because of XwikiController not being @nnotated
	// TODO : Security warning 
	public void attachment( final HttpServletRequest request, HttpServletResponse response) throws IOException  {
		// TODO : Blog
		String path = request.getServletPath().replace(XWikiHtmlService.PROXYFIED_FOLDER+"/", "");
		byte[] bytes = xwikiFacadeService.downloadAttachment(path);
		response.setContentType(xwikiFacadeService.detectMimeType(path));
		// TODO : Have a streamed version
		response.getOutputStream().write(bytes);
	}
	
}
