package org.open4goods.model.vertical;

public record WikiPageConfig(String wikiUrl, String verticalUrl, String title, String faIcon) {

        public WikiPageConfig() {
                this(null, null, null, null);
        }
	public String getWikiUrl() {
		return wikiUrl;
	}
	public String getVerticalUrl() {
		return verticalUrl;
	}
	public String getTitle() {
		return title;
	}
	public String getFaIcon() {
		return faIcon;
	}
	
	
    	
}
