package org.open4goods.nudgerfrontapi.config.properties;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties exposing the list of ecosystem partners rendered by the frontend.
 */
@Validated
@ConfigurationProperties(prefix = "front.partners.ecosystem")
public class EcosystemPartnersProperties {

    /**
     * Ecosystem partner entries configured in application properties.
     */
    @Valid
    private List<Partner> partners = new ArrayList<>();

    public List<Partner> getPartners() {
        return partners;
    }

    public void setPartners(List<Partner> partners) {
        this.partners = partners == null ? new ArrayList<>() : new ArrayList<>(partners);
    }

    /**
     * Single ecosystem partner entry.
     */
    public static class Partner {

        /**
         * Display name of the ecosystem partner.
         */
        @NotBlank(message = "front.partners.ecosystem.partners[].name must be provided")
        private String name;

        /**
         * XWiki bloc identifier linking to partner content.
         */
        @NotBlank(message = "front.partners.ecosystem.partners[].bloc-id must be provided")
        private String blocId;

        /**
         * Public website URL of the partner.
         */
        @NotBlank(message = "front.partners.ecosystem.partners[].url must be provided")
        private String url;

        /**
         * Relative image URL rendered in the frontend.
         */
        @NotBlank(message = "front.partners.ecosystem.partners[].image-url must be provided")
        private String imageUrl;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getBlocId() {
            return blocId;
        }

        public void setBlocId(String blocId) {
            this.blocId = blocId;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getImageUrl() {
            return imageUrl;
        }

        public void setImageUrl(String imageUrl) {
            this.imageUrl = imageUrl;
        }
    }
}
