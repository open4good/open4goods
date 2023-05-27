package org.open4goods.ui.controllers.ui;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.Charset;

import org.open4goods.helper.IpHelper;
import org.open4goods.model.data.AffiliationToken;
import org.open4goods.services.SerialisationService;
import org.open4goods.ui.repository.AffiliationTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@RestController
//* TODO(design,P2,0.25) : should make an AffiliationService
public class AffiliatedController {

	private static final Logger logger = LoggerFactory.getLogger(AffiliatedController.class);

	private @Autowired SerialisationService serialisationService;

	private @Autowired AffiliationTokenRepository repository;

	@GetMapping("/compensation/{token}")
	public ModelAndView compensation(@PathVariable(required = true, name = "token") final String token,
			final HttpServletRequest request, final HttpServletResponse response) {


		// No follow
		response.addHeader("X-Robots-Tag", "noindex, nofollow");

		
		// TODO : in a service
		final String ip = IpHelper.getIp(request);

		String str = null;
		try {
			str = SerialisationService.uncompressString(URLDecoder.decode(token,Charset.defaultCharset()));
		} catch (final IOException e) {
			logger.warn("Error uncompressing {}", token, e);
		}
		AffiliationToken aff = null;
		try {

			// Setting ip and user agent
			aff = serialisationService.fromJson(str, AffiliationToken.class);
			aff.setIp(ip);
			aff.setUa(request.getHeader("User-Agent"));
		} catch (final Exception e) {
			logger.warn("Error generating AffiliationToken {}", token, e);
		}


		// TODO(P2,perf,0.5) : bulk, delay
		repository.save(aff);
		
		// Redirecting user pageSize the offer
		RedirectView rv = new RedirectView();
		rv.setStatusCode(HttpStatus.MOVED_TEMPORARILY);
		rv.setUrl(aff.getUrl());
		ModelAndView mv = new ModelAndView(rv);
		return mv;
	}
}
