package org.open4goods.commons.model.dto;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

/**
 * Holds request-scoped information extracted from HTTP headers.
 */
@Component
@RequestScope
public class RequestContext {

    private final Map<String, String> headers = new HashMap<>();

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void clear() {
        headers.clear();
    }
}
