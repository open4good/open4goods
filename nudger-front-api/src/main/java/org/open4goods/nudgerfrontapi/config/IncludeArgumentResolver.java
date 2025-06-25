package org.open4goods.nudgerfrontapi.config;

import java.lang.reflect.ParameterizedType;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Resolves the optional {@code include} query parameter into a {@link Set} of
 * field names. The resulting set is also stored as a request attribute so that
 * Jackson filters can access it later on.
 */
@Component
public class IncludeArgumentResolver implements HandlerMethodArgumentResolver {

    /** Request attribute holding the parsed includes set. */
    public static final String ATTRIBUTE_NAME = IncludeArgumentResolver.class.getName() + ".INCLUDES";

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        if (!Set.class.isAssignableFrom(parameter.getParameterType())) {
            return false;
        }
        if (!(parameter.getGenericParameterType() instanceof ParameterizedType type)) {
            return false;
        }
        if (!type.getActualTypeArguments()[0].equals(String.class)) {
            return false;
        }
        return "includes".equals(parameter.getParameterName());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        String raw = webRequest.getParameter("include");
        if (raw == null || raw.isBlank()) {
            Set<String> empty = Collections.emptySet();
            webRequest.setAttribute(ATTRIBUTE_NAME, empty, RequestAttributes.SCOPE_REQUEST);
            return empty;
        }
        Set<String> includes = new LinkedHashSet<>(Arrays.asList(raw.split(",")));
        includes.removeIf(String::isBlank);
        webRequest.setAttribute(ATTRIBUTE_NAME, includes, RequestAttributes.SCOPE_REQUEST);
        return includes;
    }
}
