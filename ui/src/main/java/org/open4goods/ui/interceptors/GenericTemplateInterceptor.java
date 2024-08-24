package org.open4goods.ui.interceptors;

import org.open4goods.commons.model.dto.PageInterceptionResponse;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class GenericTemplateInterceptor implements HandlerInterceptor {

	@Override
	public boolean preHandle(final HttpServletRequest request, final HttpServletResponse response, final Object handler)
			throws Exception {
		request.setAttribute("initiated", System.currentTimeMillis());
		return true;
	}

	@Override
	public void postHandle(final HttpServletRequest request, final HttpServletResponse response, final Object handler,
			final ModelAndView modelAndView) throws Exception {
		if (null != modelAndView) {
			final PageInterceptionResponse genericResponse = new PageInterceptionResponse((Long) request.getAttribute("initiated"));
			modelAndView.addObject("genericResponse", genericResponse);
		}
	}

}
