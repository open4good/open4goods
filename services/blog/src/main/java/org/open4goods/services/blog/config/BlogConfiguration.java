package org.open4goods.services.blog.config;

import org.open4goods.model.Localisable;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration for an XWiki connexion
 * @author goulven
 *
 */
@Component
@ConfigurationProperties(prefix = "blog")
public class BlogConfiguration {


	private String feedType = "rss_2.0";
	private String blogUrl = "blog/";
	private String feedUrl = "blog/rss/";

	// If set, this domain prefix will be used with all blog
	// resources
	private String staticDomain;

	private Localisable<String,String> feedTitle = new Localisable<String,String>();
	private Localisable<String,String> feedDescription = new Localisable<String,String>();


	public String getFeedType() {
		return feedType;
	}
	public void setFeedType(String feedType) {
		this.feedType = feedType;
	}


	public Localisable<String, String> getFeedTitle() {
		return feedTitle;
	}
	public void setFeedTitle(Localisable<String, String> feedTitle) {
		this.feedTitle = feedTitle;
	}
	public Localisable<String, String> getFeedDescription() {
		return feedDescription;
	}
	public void setFeedDescription(Localisable<String, String> feedDescription) {
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
	public String getStaticDomain() {
		return staticDomain;
	}
	public void setStaticDomain(String staticDomain) {
		this.staticDomain = staticDomain;
	}




}