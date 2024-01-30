package org.open4goods.ui.controllers.ui;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.open4goods.services.FeedbackService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.http.HttpServletRequest;

@Controller
public class FeedbackController extends AbstractUiController {

	private static final Logger LOGGER = LoggerFactory.getLogger(FeedbackController.class);

	private FeedbackService feedbackService;

	public FeedbackController(FeedbackService feedbackService) {
		this.feedbackService = feedbackService;
	}

//	public ModelAndView issue(final HttpServletRequest request,  @RequestParam("message") String message, @RequestParam("url") String urlSource, @RequestParam("author") String author, @RequestParam("author") String captcha) {

	@GetMapping("/feedback/issue")
	public ModelAndView issue(final HttpServletRequest request,  @RequestParam(required = false, name = "url") String url) {

		
//		if (captcha == null || !captcha.equals("1")) {
//			LOGGER.warn("Captcha is not valid");
//			return defaultModelAndView("feedback-error", request);
//		}
//		
		
		
		
		ModelAndView model = defaultModelAndView("feedback-issue", request);
		model.addObject("urlSource",url);
		
		return model;
	}

	@PostMapping("/feedback/issue")
	public ModelAndView createIssue(final HttpServletRequest request, @RequestBody MultiValueMap<String, String> formData) {

		if (formData == null) {
			LOGGER.warn("No form data");
			return defaultModelAndView("feedback-error", request);
		}
		
		if (formData.getFirst("captcha") == null || !formData.getFirst("captcha").equals("1")) {
			LOGGER.warn("Captcha is not valid");
			return defaultModelAndView("feedback-error", request);
		}


		try {
			Set<String> labels = new HashSet<String>();
			labels.add("feedback");
			labels.add("issue");
			
			feedbackService.createIssue(formData.getFirst("title"),formData.getFirst("message"), formData.getFirst("url"), formData.getFirst("author"), labels);
			
		
		} catch (IOException e) {
			LOGGER.error("Error while creating issue", e);
			return defaultModelAndView("feedback-error", request);
		}
		
		
		
		
		return defaultModelAndView("feedback-success", request);
	}
	
	@GetMapping("/feedback/idea")
	public ModelAndView idea(final HttpServletRequest request) {

		ModelAndView model = defaultModelAndView("feedback-idea", request);
		
		return model;
	}


	
	
}
