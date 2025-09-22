package org.open4goods.xwiki.model;

import java.util.HashMap;
import java.util.Map;

import org.xwiki.rest.model.jaxb.Objects;
import org.xwiki.rest.model.jaxb.Page;

import io.swagger.v3.oas.annotations.media.Schema;

public class FullPage {

        private String htmlContent;
        private Page wikiPage;
        private Objects objects;
        private Map<String, String> properties = new HashMap<>();

        @Schema(description = "Language used to render the page", example = "fr")
        private String resolvedLanguage;
	
	
	public String getProp(String string) {
		return properties.get(string);
	}
	
	
	
	public String getHtmlContent() {
		return htmlContent;
	}
	public void setHtmlContent(String htmlContent) {
		this.htmlContent = htmlContent;
	}
	public Page getWikiPage() {
		return wikiPage;
	}
	public void setWikiPage(Page wikiPage) {
		this.wikiPage = wikiPage;
	}
	public Objects getObjects() {
		return objects;
	}
	public void setObjects(Objects objects) {
		this.objects = objects;
	}
        public Map<String, String> getProperties() {
                return properties;
        }
        public void setProperties(Map<String, String> properties) {
                this.properties = properties;
        }

        public String getResolvedLanguage() {
                return resolvedLanguage;
        }

        public void setResolvedLanguage(String resolvedLanguage) {
                this.resolvedLanguage = resolvedLanguage;
        }

	
	
	
}
