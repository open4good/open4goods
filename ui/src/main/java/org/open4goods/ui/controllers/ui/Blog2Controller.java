package org.open4goods.ui.controllers.ui;

import java.util.ArrayList;
import java.util.List;

import org.open4goods.dao.ProductRepository;
import org.open4goods.model.blog.BlogPost2;
import org.open4goods.services.DataSourceConfigService;
import org.open4goods.services.VerticalsConfigService;
import org.open4goods.ui.services.Blog2Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;


import jakarta.servlet.http.HttpServletRequest;

@Controller
public class Blog2Controller extends AbstractUiController {

	private static final Logger LOGGER = LoggerFactory.getLogger(Blog2Controller.class);

	private final ProductRepository aggregatedDataRepository;
	private final DataSourceConfigService datasourceConfigService;

	private final VerticalsConfigService verticalConfigService;

	
	private Blog2Service blogService;

	public Blog2Controller(ProductRepository aggregatedDataRepository, DataSourceConfigService datasourceConfigService, VerticalsConfigService verticalConfigService, Blog2Service xwikiService) {
		this.aggregatedDataRepository = aggregatedDataRepository;
		this.datasourceConfigService = datasourceConfigService;
		this.verticalConfigService = verticalConfigService;
		this.blogService = xwikiService;
	}


	@GetMapping("/blog2")
	public ModelAndView blog2Index(final HttpServletRequest request) {

		// TODO : Remove this test page
		ModelAndView model = defaultModelAndView("blog2", request);

		model.addObject("totalItems", aggregatedDataRepository.countMainIndex());

		// TODO(gof) : deduplicate (darty.com / darty.com-CSV)
		model.addObject("partners",  datasourceConfigService.datasourceConfigs().size());

		model.addObject("verticals",  verticalConfigService.getConfigsWithoutDefault());

		model.addObject("url",  "/");

		
		List<BlogPost2> posts = new ArrayList<>(blogService.getBlogPosts().values());
		model.addObject("posts", posts);
		
		
		return model;
	}

//
//	@GetMapping(value="/blog/rss",  produces = "application/xml")
//	public void rss(HttpServletResponse response, HttpServletRequest request ) throws FeedException, IOException {
//
//		response.setContentType("application/rss+xml");
//		response.setCharacterEncoding("UTF-8");
//		response.setHeader("Cache-Control", "no-cache");
//		response.setHeader("Pragma", "no-cache");
//		response.setDateHeader("Expires", -1);
//		response.getWriter().write(blogService.rss(request.getLocale().getLanguage()));
//		
//	}
	
	
//	@GetMapping("/blog/{post}")
//	public ModelAndView post(@PathVariable String post, final HttpServletRequest request) {
//
//		// TODO : Remove this test page
//		ModelAndView model = defaultModelAndView("blog-post", request);
//
//		model.addObject("totalItems", aggregatedDataRepository.countMainIndex());
//
//		// TODO(gof) : deduplicate (darty.com / darty.com-CSV)
//		model.addObject("partners",  datasourceConfigService.datasourceConfigs().size());
//
//		model.addObject("verticals",  verticalConfigService.getConfigsWithoutDefault());
//
//		model.addObject("url",  "/");
//		
//		
//		BlogPost blogPost = blogService.getBlogPosts().get(post);
//		
//		if (null == blogPost) {
//			LOGGER.error("Blog post not found : {}", post);
//			// TODO : Throw a 404
//			return new ModelAndView("redirect:/blog");
//		}
//		model.addObject("post", blogPost);
//		
//		
////		model.addObject("pages", pages);
//		return model;
//	}

	
}
