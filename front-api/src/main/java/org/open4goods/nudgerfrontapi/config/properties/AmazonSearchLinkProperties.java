package org.open4goods.nudgerfrontapi.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration of the "Search on Amazon" link shown on product pages without any offer (GOU-1396).
 * <p>
 * The link is an Amazon Associates search by EAN, routed through {@code /contrib/} so the click is recorded. None of
 * these values is a secret: the Associates tag is public by design (it is part of every link).
 * </p>
 */
@ConfigurationProperties(prefix = "front.amazon-search-link")
public class AmazonSearchLinkProperties {

    /** Placeholder replaced by the product GTIN in {@link #urlTemplate}. */
    public static final String GTIN_PLACEHOLDER = "{gtin}";

    /** Placeholder replaced by the Associates tag in {@link #urlTemplate}. */
    public static final String TAG_PLACEHOLDER = "{tag}";

    /** Whether the link is exposed at all. */
    private boolean enabled = true;

    /** Amazon Associates tag appended to the search URL. */
    private String tag = "nudger-21";

    /** Search URL template, with the {@code {gtin}} and {@code {tag}} placeholders. */
    private String urlTemplate = "https://www.amazon.fr/s?k=" + GTIN_PLACEHOLDER + "&tag=" + TAG_PLACEHOLDER;

    /** Merchant (datasource name) recorded with the click and carried by the {@code /contrib/} token. */
    private String merchant = "amazon.fr";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getUrlTemplate() {
        return urlTemplate;
    }

    public void setUrlTemplate(String urlTemplate) {
        this.urlTemplate = urlTemplate;
    }

    public String getMerchant() {
        return merchant;
    }

    public void setMerchant(String merchant) {
        this.merchant = merchant;
    }
}
