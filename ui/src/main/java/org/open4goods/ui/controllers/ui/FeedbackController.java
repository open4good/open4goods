package org.open4goods.ui.controllers.ui;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.open4goods.exceptions.InvalidParameterException;
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

	@GetMapping("/feedback/issue")
	public ModelAndView issue(final HttpServletRequest request,  @RequestParam(required = false, name = "url") String url) {
		
		ModelAndView model = defaultModelAndView("feedback-issue", request);

		// Adding authenticated user
		if (null != model.getModel().get("user")) {
			model.addObject("author", model.getModel().get("user"));
		}
		model.addObject("urlSource",url);
		
		return model;
	}

	@GetMapping("/feedback/idea")
	public ModelAndView idea(final HttpServletRequest request,  @RequestParam(required = false, name = "url") String url) {
		
		ModelAndView model = defaultModelAndView("feedback-idea", request);

		// Adding authenticated user
		if (null != model.getModel().get("user")) {
			model.addObject("author", model.getModel().get("user"));
		}
		model.addObject("urlSource",url);
		
		return model;
	}
	
	
	@PostMapping("/feedback")
	public ModelAndView createIssue(final HttpServletRequest request, @RequestBody MultiValueMap<String, String> formData) {

		if (formData == null) {
			LOGGER.warn("No form data");
			
			return defaultModelAndView("feedback-error", request).addObject("msg","No form data");
		}
		
		if (formData.getFirst("captcha") == null || !formData.getFirst("captcha").equals("1")) {
			LOGGER.warn("Captcha is not valid");
			return defaultModelAndView("feedback-error", request).addObject("msg","Invalid captcha");
		}


		try {
			Set<String> labels = new HashSet<String>();
			// TODO : from conf
			labels.add("nudger.fr");
			labels.add("feedback");

			if (formData.getFirst("type").equals("bug")) {
				labels.add("bug");
				
			} else if (formData.getFirst("type").equals("idea")) {
				labels.add("feature");
			} 
			
			String url = formData.getFirst("url");
			if (url.equals("/")) {
				url = config.getBaseUrl(request.getLocale());
			}
			
			feedbackService.createBug(formData.getFirst("title"),formData.getFirst("message"), url, formData.getFirst("author"), labels);
			
		
		} catch (IOException e) {
			LOGGER.error("Error while creating issue", e);
			return defaultModelAndView("feedback-error", request).addObject("msg","Internal exception : "+e.getMessage());
		} catch (InvalidParameterException e) {
			LOGGER.error("Error while creating issue, invalid parameters", e);
			return defaultModelAndView("feedback-error", request).addObject("msg","Invalid parameter exception : "+e.getMessage());
		}
		
		return defaultModelAndView("feedback-success", request).addObject("backUrl",formData.getFirst("url"));
	}


	
	
}
