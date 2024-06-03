package org.open4goods.ui.controllers.ui.pages;

import org.open4goods.helper.IpHelper;
import org.open4goods.services.MailService;
import org.open4goods.services.RecaptchaService;
import org.open4goods.ui.config.yml.UiConfig;
import org.open4goods.ui.controllers.ui.UiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import cz.jiripinkas.jsitemapgenerator.ChangeFreq;
import jakarta.servlet.http.HttpServletRequest;

@Controller
public class ContactController implements SitemapExposedController{

	public static final String DEFAULT_PATH="/contact";
		
	private final MailService mailService;
	private @Autowired UiService uiService;
	private final UiConfig uiConfig;

	private final RecaptchaService captchaService;

	public ContactController(MailService mailService, UiConfig uiConfig, RecaptchaService captchaService) {
		this.mailService = mailService;
		this.uiConfig = uiConfig;
		this.captchaService = captchaService;
	}

	
	public SitemapEntry getExposedUrls() {
		return SitemapEntry.of(SitemapEntry.LANGUAGE_DEFAULT, DEFAULT_PATH, 0.1, ChangeFreq.YEARLY);
	}

	
	
	@GetMapping("/contact")
	public ModelAndView index(final HttpServletRequest request) {
		ModelAndView model = uiService.defaultModelAndView("contact", request);
		model.addObject("page","nous contacter");
		return model;
	}

	@PostMapping("/contact")
	public ModelAndView index(@RequestParam String name, @RequestParam String email, @RequestParam String message,@RequestParam(name="h-captcha-response") String recaptchaResponse,	final HttpServletRequest request) {

		ModelAndView model = uiService.defaultModelAndView("contact", request);
		model.addObject("page","nous contacter");
		try {
			captchaService.verifyRecaptcha(IpHelper.getIp(request), recaptchaResponse);
			mailService.sendEmail(uiConfig.getEmail(), message, "nudger.fr > Message de " + name, email);
			model.addObject("info", "Votre message a bien été envoyé !");
		} catch (Exception e) {
			model.addObject("error", "Le mail n'a pas pu être envoyé : " + e.getMessage());
		}

		return model;
	}


}
