package org.open4goods.ui.controllers.ui;

import org.open4goods.helper.IpHelper;
import org.open4goods.services.MailService;
import org.open4goods.services.RecaptchaService;
import org.open4goods.ui.config.yml.UiConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.http.HttpServletRequest;

@Controller
public class ContactController extends AbstractUiController {

	private final MailService mailService;

	private final UiConfig uiConfig;

	private final RecaptchaService captchaService;

	public ContactController(MailService mailService, UiConfig uiConfig, RecaptchaService captchaService) {
		this.mailService = mailService;
		this.uiConfig = uiConfig;
		this.captchaService = captchaService;
	}

	@GetMapping("/contact")
	public ModelAndView index(final HttpServletRequest request) {

		ModelAndView model = defaultModelAndView("contact", request);
		model.addObject("page","nous contacter");
		return model;
	}

	@PostMapping("/contact")
	public ModelAndView index(@RequestParam String name, @RequestParam String email, @RequestParam String message,@RequestParam(name="g-recaptcha-response") String recaptchaResponse,	final HttpServletRequest request) {

		ModelAndView model = defaultModelAndView("contact", request);
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
