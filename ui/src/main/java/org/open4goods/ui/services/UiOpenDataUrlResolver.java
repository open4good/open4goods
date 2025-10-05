package org.open4goods.ui.services;

import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.open4goods.model.product.Product;
import org.open4goods.services.opendata.url.OpenDataUrlResolver;
import org.open4goods.ui.config.yml.UiConfig;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * UI specific {@link OpenDataUrlResolver} that relies on {@link UiConfig} to build product URLs.
 */
@Component
@Primary
public class UiOpenDataUrlResolver implements OpenDataUrlResolver {

    private final UiConfig uiConfig;

    public UiOpenDataUrlResolver(UiConfig uiConfig) {
        this.uiConfig = uiConfig;
    }

    @Override
    public String resolve(Product product, Locale locale) {
        if (product == null || product.getNames() == null || product.getNames().getUrl() == null) {
            return null;
        }

        Map<String, String> urls = product.getNames().getUrl();
        String language = locale != null ? locale.getLanguage() : Locale.getDefault().getLanguage();
        String path = urls.get(language);
        if (StringUtils.isBlank(path)) {
            path = urls.get("default");
        }
        if (StringUtils.isBlank(path)) {
            return null;
        }

        String baseUrl = uiConfig.getBaseUrl(locale != null ? locale : Locale.getDefault());
        if (StringUtils.isBlank(baseUrl)) {
            return null;
        }

        return StringUtils.appendIfMissing(baseUrl, "/") + StringUtils.removeStart(path, "/");
    }
}
