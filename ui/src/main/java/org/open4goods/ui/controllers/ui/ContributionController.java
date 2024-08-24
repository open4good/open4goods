package org.open4goods.ui.controllers.ui;

import org.apache.commons.lang3.StringUtils;
import org.open4goods.commons.helper.IpHelper;
import org.open4goods.ui.config.yml.UiConfig;
import org.open4goods.ui.controllers.ui.pages.SitemapEntry;
import org.open4goods.ui.controllers.ui.pages.SitemapExposedController;
import org.open4goods.ui.services.ContributionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import cz.jiripinkas.jsitemapgenerator.ChangeFreq;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * This controller is in charge of the user contribution votes 
 */
@Controller
public class ContributionController implements SitemapExposedController{

	private static final Logger logger = LoggerFactory.getLogger(ContributionController.class);

	
	public static final String DEFAULT_PATH="/ecological-compensation";
	public static final String FR_PATH="/compensation-ecologique";
	
	// Max length a user agent header can have
	private static final int MAX_UA_LENGTH = 1000;

	

	private ContributionService contributionService;
	private UiService uiService;
	
	public ContributionController(ContributionService contributionService, UiService uiService ) {
		super();
		this.contributionService = contributionService;
		this.uiService = uiService;
	}

	/**
	 * Method in charge of stroring the nudges (contribution vote)
	 * @param token
	 * @param vote
	 * @param request
	 * @param response
	 * @return
	 */
	@PostMapping("/nudge")
	public ModelAndView nudge(@RequestParam(required = true) final String token,
									 @RequestParam(required = true,  name = "nudge") final String vote,
									 final HttpServletRequest request, final HttpServletResponse response) {

		// Setting the no follow header
		response.addHeader("X-Robots-Tag", "noindex, nofollow");
		
		// Get IP and UA
		String ip = IpHelper.getIp(request);
		String ua = request.getHeader("User-Agent");
		
		// Avoid possibility of "db spoofing"
		if (ua.length() > MAX_UA_LENGTH) {
			ua = ua.substring(0, MAX_UA_LENGTH);
		}
		
		// Operates the user contribution vote 
		String url = contributionService.processContributionVote(ip, ua, token, vote);
		
		ModelAndView mv;
		if (StringUtils.isEmpty(url)) {
			// An error, can not record user choice
			logger.error("Can not record user contribution vote, for vote {} and token {} ", vote, token);
			
			RedirectView rv = new RedirectView("/error/500");
			rv.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
			rv.setUrl(url);
			mv = new ModelAndView(rv);
		} else {
			// Redirecting user to the offer
			RedirectView rv = new RedirectView();
			rv.setStatusCode(HttpStatus.FOUND);
			rv.setUrl(url);
			mv = new ModelAndView(rv);
		}
		
		return mv;
	}
	
	/**
	 * Presentation page
	 * @param request
	 * @return
	 */
	@GetMapping(value = {DEFAULT_PATH, FR_PATH})
	public ModelAndView compensation(final HttpServletRequest request) {
		ModelAndView ret = uiService.defaultModelAndView(("compensation"), request);
		
		ret.addObject("votes", contributionService.nudgesCountSinceLastReversement());
		ret.addObject("repartition", contributionService.nudgesRepartitionSinceLastReversement());
		
		ret.addObject("page","compensation écologique");
		
		return ret;
	}

	@Override
	public SitemapEntry getExposedUrls() {
		return SitemapEntry.of(SitemapEntry.LANGUAGE_DEFAULT, DEFAULT_PATH, 0.3, ChangeFreq.YEARLY)
				.add(SitemapEntry.LANGUAGE_FR, FR_PATH);
	}
	
}
