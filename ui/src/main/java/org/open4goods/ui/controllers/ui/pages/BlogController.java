package org.open4goods.ui.controllers.ui.pages;

import java.io.IOException;
import java.util.List;

import org.open4goods.ui.config.yml.UiConfig;
import org.open4goods.ui.controllers.ui.UiService;
import org.open4goods.services.blog.model.BlogPost;
import org.open4goods.services.blog.service.BlogService;
import org.open4goods.xwiki.services.XWikiHtmlService;
import org.open4goods.xwiki.services.XwikiFacadeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;

import com.rometools.rome.io.FeedException;

import cz.jiripinkas.jsitemapgenerator.ChangeFreq;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Controller
public class BlogController  implements SitemapExposedController{

	public static final String DEFAULT_PATH="/blog";

	private static final Logger LOGGER = LoggerFactory.getLogger(BlogController.class);

	private final XwikiFacadeService xwikiFacadeService;
	private BlogService blogService;
	private UiService uiService;
	
	public BlogController( BlogService blogService, XwikiFacadeService xwikiFacadeService, UiService uiService) {
		this.blogService = blogService;
		this.xwikiFacadeService = xwikiFacadeService;
		this.uiService = uiService;
	}

	@Override
	public SitemapEntry getExposedUrls() {
		return SitemapEntry.of(SitemapEntry.LANGUAGE_DEFAULT, DEFAULT_PATH, 0.5, ChangeFreq.WEEKLY);
	}
	
	/**
	 * Blog entry page
	 * @param request
	 * @param tag
	 * @return
	 */
	@GetMapping(DEFAULT_PATH)
	public ModelAndView blogIndex(final HttpServletRequest request) {
		ModelAndView model = uiService.defaultModelAndView("blog", request);
		model.addObject("url",  "/");
		List<BlogPost> posts = blogService.getPosts();
		
		model.addObject("posts", posts);
		model.addObject("tags",blogService.getTags());
		return model;
	}


	/**
	 * Blog entry page
	 * @param request
	 * @param tag
	 * @return
	 */
	@GetMapping(DEFAULT_PATH +  "/tag/{tag}")
	public ModelAndView blogIndexByCat(final HttpServletRequest request, @PathVariable String tag) {
		ModelAndView model = uiService.defaultModelAndView("blog", request);
		model.addObject("url",  "/");
		List<BlogPost> posts = blogService.getPosts();
		
		// Filtering by tag
		if (null != tag) {
			posts = posts.stream().filter(e->e.getCategory().contains(tag)).toList();
		}
		
		model.addObject("posts", posts);	
		model.addObject("tags",blogService.getTags());
		model.addObject("currentTag", tag);
		return model;
	}

	
	
	
	
	/**
	 * Blog post page
	 * @param post
	 * @param request
	 * @return
	 */
	@GetMapping(DEFAULT_PATH + "/{post}")
	public ModelAndView post(@PathVariable String post, final HttpServletRequest request) {
		ModelAndView model = uiService.defaultModelAndView("blog-post", request);

		BlogPost blogPost = blogService.getPostsByUrl().get(post);
		
		if (null == blogPost) {
			LOGGER.info("Blog post not found : {}", post);
			throw new ResponseStatusException( HttpStatus.NOT_FOUND, "Unable to find blog post");
		} else {
			model.addObject("post", blogPost);	
			return model;			
		}
	}

	/**
	 * Retrieve the cover image for a blog post
	 * @param page
	 * @param filename
	 * @param request
	 * @param response
	 * @throws IOException
	 */
	@GetMapping(DEFAULT_PATH + "/{page}/{filename}")	
	// TODO(p3,perf) : Caching
	public void attachment( @PathVariable(name = "page") String page, @PathVariable(name = "filename") String filename, final HttpServletRequest request, HttpServletResponse response) throws IOException  {
		byte[] bytes = xwikiFacadeService.downloadAttachment("Blog", page, filename);
		response.setContentType(xwikiFacadeService.detectMimeType(filename));
		// TODO(p3,perf) : Have a streamed version
		response.getOutputStream().write(bytes);
	}
	
	
	@GetMapping(XWikiHtmlService.PROXYFIED_FOLDER+ "/**")	
	// TODO(p3,design) : classical xwiki download content is served here and it shouldn't, because of XwikiController not being @nnotated
	// TODO(p3,perf) : Caching
	public void attachment( final HttpServletRequest request, HttpServletResponse response) throws IOException  {
		String path = request.getServletPath().replace(XWikiHtmlService.PROXYFIED_FOLDER+"/", "");
		byte[] bytes = xwikiFacadeService.downloadAttachment(path);
		response.setContentType(xwikiFacadeService.detectMimeType(path));
		// TODO(p3,perf) : Have a streamed version
		response.getOutputStream().write(bytes);
	}
	
	
	/**
	 * RSS Feed url
	 * @param response
	 * @param request
	 * @throws FeedException
	 * @throws IOException
	 */
	@GetMapping(value=DEFAULT_PATH+"/rss",  produces = "application/xml")
	public void rss(HttpServletResponse response, HttpServletRequest request ) throws FeedException, IOException {
		response.setContentType("application/rss+xml");
		response.setCharacterEncoding("UTF-8");
		response.setHeader("Cache-Control", "no-cache");
		response.setHeader("Pragma", "no-cache");
		response.setDateHeader("Expires", -1);
		response.getWriter().write(blogService.rss(uiService.getSiteLanguage(request)));		
	}
	
	
}
