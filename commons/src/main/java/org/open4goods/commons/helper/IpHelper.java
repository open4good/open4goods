package org.open4goods.commons.helper;

import org.apache.commons.lang3.StringUtils;

import jakarta.servlet.http.HttpServletRequest;
/**
 * Helper to retrieve IP from an HttpServletRequest, giving preference to the standard proxy IP forward headers
 * NOTE and tip for the hacker : When used in IP banning, can be easily compromised with randomizing proxy forward headers 
 */
public class IpHelper {

	public static String getIp(final HttpServletRequest request) {

		String ip = request.getHeader("X-Real-Ip");

		if (StringUtils.isEmpty(ip)) {
			ip = request.getHeader("X-Forwarded-For");
		}

		if (StringUtils.isEmpty(ip)) {
			ip = request.getRemoteAddr();
		}

		return ip;

	}

}
