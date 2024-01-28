package org.open4goods.config.yml;

import org.open4goods.model.Localisable;

/**
 * Configuration for an XWiki connexion
 * @author goulven
 *
 */
public class BlogConfiguration {
	
	
	private String feedType = "rss_1.0";
	private String blogUrl = "blog/";
	private String feedUrl = "blog/rss/";
	
	private Localisable feedTitle = new Localisable();
	private Localisable feedDescription = new Localisable();
	
	
	public String getFeedType() {
		return feedType;
	}
	public void setFeedType(String feedType) {
		this.feedType = feedType;
	}
	public Localisable getFeedTitle() {
		return feedTitle;
	}
	public void setFeedTitle(Localisable feedTitle) {
		this.feedTitle = feedTitle;
	}
	public Localisable getFeedDescription() {
		return feedDescription;
	}
	public void setFeedDescription(Localisable feedDescription) {
		this.feedDescription = feedDescription;
	}
	public String getBlogUrl() {
		return blogUrl;
	}
	public void setBlogUrl(String blogUrl) {
		this.blogUrl = blogUrl;
	}
	public String getFeedUrl() {
		return feedUrl;
	}
	public void setFeedUrl(String feedUrl) {
		this.feedUrl = feedUrl;
	}
	
	
	
	
}