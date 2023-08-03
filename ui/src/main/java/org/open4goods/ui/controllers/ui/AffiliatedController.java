package org.open4goods.ui.controllers.ui;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.Charset;

import org.open4goods.helper.IpHelper;
import org.open4goods.model.constants.CacheConstants;
import org.open4goods.model.data.AffiliationToken;
import org.open4goods.services.SerialisationService;
import org.open4goods.ui.repository.AffiliationTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.Cache.ValueWrapper;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

	private @Autowired CacheManager cacheManager;
	
	
	@PostMapping("/nudge")
	public ModelAndView compensation(@RequestParam(required = true,  name = "token") final String token,
			@RequestParam(required = true,  name = "nudge") final String target,
			final HttpServletRequest request, final HttpServletResponse response) {

		Cache cache = cacheManager.getCache(CacheConstants.ONE_HOUR_LOCAL_CACHE_NAME);
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
			ValueWrapper existantVote = cache.get(ip);
			
			if (null == existantVote) {
				aff.setCashback(target);
			} else {
				aff.setCashback("DUPLICATE-IP");
			}
			cache.put(ip, target);
			
			// TODO(P2,perf,0.5) : bulk, delay
			repository.save(aff);
			
		} catch (final Exception e) {
			logger.warn("Error generating AffiliationToken {}", token, e);
		}
		

		// Redirecting user pageSize the offer
		RedirectView rv = new RedirectView();
		rv.setStatusCode(HttpStatus.MOVED_TEMPORARILY);
		rv.setUrl(aff.getUrl());
		ModelAndView mv = new ModelAndView(rv);
		return mv;
	}
}
