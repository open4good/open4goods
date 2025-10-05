package org.open4goods.services.opendata.url;

import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.open4goods.model.product.Product;
import org.open4goods.services.opendata.config.OpenDataConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Default implementation that relies on {@link OpenDataConfig} to build product URLs.
 */
@Component
public class DefaultOpenDataUrlResolver implements OpenDataUrlResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultOpenDataUrlResolver.class);

    private final OpenDataConfig openDataConfig;

    public DefaultOpenDataUrlResolver(OpenDataConfig openDataConfig) {
        this.openDataConfig = openDataConfig;
    }

    @Override
    public String resolve(Product product, Locale locale) {
        if (product == null || product.getNames() == null || product.getNames().getUrl() == null) {
            return null;
        }

        Map<String, String> urls = product.getNames().getUrl();
        String path = urls.get(locale.getLanguage());
        if (StringUtils.isBlank(path)) {
            path = urls.get("default");
        }
        if (StringUtils.isBlank(path)) {
            return null;
        }

        String baseUrl = openDataConfig.getBaseUrl(locale);
        if (StringUtils.isBlank(baseUrl)) {
            LOGGER.debug("No base URL configured for locale {}", locale);
            return null;
        }

        return StringUtils.appendIfMissing(baseUrl, "/") + StringUtils.removeStart(path, "/");
    }
}
