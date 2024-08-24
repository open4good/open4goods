package org.open4goods.commons.interceptors;

import org.open4goods.commons.config.yml.BanCheckerConfig;
import org.open4goods.commons.helper.IpHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
/**
 * This interceptor will ban request, bases on list of IP and UA
 */
public class BanCheckerInterceptor implements HandlerInterceptor {

	protected static final Logger logger = LoggerFactory.getLogger(BanCheckerInterceptor.class);
	
	private BanCheckerConfig bancheckerConfig;

	public BanCheckerInterceptor(BanCheckerConfig bancheckerConfig) {
		this.bancheckerConfig = bancheckerConfig;
	}

	@Override
	public boolean preHandle(final HttpServletRequest request, final HttpServletResponse response, final Object handler)
			throws Exception {
	
		// Retrieving IP
		String ip = IpHelper.getIp(request);
		logger.info("Check banlist for {}",ip);
		
		// Checkinf if IP is in the banlist
		if (bancheckerConfig.getIps().contains(ip)) {
			logger.info("Banning : ",ip);
			response.setStatus(403);
			return false;
		}
		
		return true;
	}

	

}
