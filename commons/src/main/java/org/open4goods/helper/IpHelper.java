package org.open4goods.helper;

import org.apache.commons.lang3.StringUtils;

import jakarta.servlet.http.HttpServletRequest;

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
