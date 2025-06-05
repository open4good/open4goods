package org.open4goods.xwiki.config;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.util.UriComponentsBuilder;
import org.xwiki.rest.model.jaxb.Link;
// TODO : Should be directly in properties class
public class UrlManagementHelper {

	private XWikiServiceProperties properties;
	
	private static Logger logger = LoggerFactory.getLogger(UrlManagementHelper.class);
	
	public UrlManagementHelper(XWikiServiceProperties properties) {
		this.properties = properties;
	}
	/**
	 * Update http scheme in https if needed
	 * @param url
	 * @return
	 */
	public String updateUrlScheme(String url) {

		String updated = url;
		if(url != null && properties.isHttpsOnly()) {
			// TODO : Should be in conf, or should be goof opn the wiki side
			updated = url.replaceFirst("http:", "https:");
		}
		return updated;
	}
	
	/**
	 * Clean url: 
	 * 				URLDecoding  
	 * 				Force https if httpsOnly set
	 * 				Add 'media' query param from properties
	 * @param url
	 * @param key
	 * @param value
	 * @return this url with query param key=value if process succedded, null otherwise
	 */
	public String cleanUrl(String url) {

		String clean = "";
		try {
			if( url != null ) {
				String secureUrl = updateUrlScheme(url);
				String decoded = URLDecoder.decode(secureUrl, Charset.defaultCharset());
				// add request param if needed
				clean = addQueryParam(decoded, "media", this.properties.getMedia());
			}
		} catch (Exception e) {
			logger.warn("Exception while updating url {}. Error Message: {}",url,  e.getMessage());
		}
		return clean;
	}
	
	/**
	 * Add query params to url
	 * @param url
	 * @param key
	 * @param value
	 * @return this url with query param key=value if process succedded, null otherwise
	 */
	public String addQueryParam(String url, String key, String value) {

		String uriWithParams = null;
		try{
			URI uri = UriComponentsBuilder.fromUriString(url)
					.queryParam(key, value)
					.build()
					.toUri();

			if(uri != null) {
				// decode again after adding query params
				uriWithParams = URLDecoder.decode(uri.toString(), Charset.defaultCharset());
			}

		} catch(Exception e) {
			logger.warn("Unable to add query params {}={} to uri {}. Error Message:{}",key, value, url, e.getMessage());
		}       
		return uriWithParams;

	}
	
	/**
	 * Get href link from rel link in 'links'  
	 * @param rel
	 * @return 'href' link from 'rel' link
	 */
	public String getHref(String rel, List<Link> links) {

		String href = null;
		
		/** TODO : Argh, a Xwiki bug here. Spaces prefix are missing for objects and attachments
		 *  @see https://jira.xwiki.org/browse/XWIKI-22440
		 * Given the page 
		 * 
			<link href="http://wiki.nudger.fr/rest/wikis/xwiki/spaces/verticals/spaces/tv/spaces/technologies-tv/pages/WebHome" rel="self"/>
		    <link href="http://wiki.nudger.fr/rest/wikis/xwiki/spaces/verticals/spaces/tv/spaces/technologies-tv" rel="http://www.xwiki.org/rel/space"/>
			<link href="http://wiki.nudger.fr/rest/wikis/xwiki/spaces/verticals/tv/technologies-tv/pages/WebHome/history" rel="http://www.xwiki.org/rel/history"/>
			<link href="http://wiki.nudger.fr/rest/wikis/xwiki/spaces/verticals/tv/technologies-tv/pages/WebHome/attachments" rel="http://www.xwiki.org/rel/attachments"/>
			<link href="http://wiki.nudger.fr/rest/wikis/xwiki/spaces/verticals/tv/technologies-tv/pages/WebHome/objects" rel="http://www.xwiki.org/rel/objects"/>
			<link href="http://wiki.nudger.fr/rest/syntaxes" rel="http://www.xwiki.org/rel/syntaxes"/>
			<link href="http://wiki.nudger.fr/rest/wikis/xwiki/classes/verticals.tv.technologies-tv.WebHome" rel="http://www.xwiki.org/rel/class"/>

		 * 
		 * we operate a manual suffix appending if objects or attachments is required 
		 */
		
		
		if (rel.equals("http://www.xwiki.org/rel/objects")) {
			href = getHref("self", links) + "/objects";
		} else if (rel.equals("http://www.xwiki.org/rel/attachments")) {
			href = getHref("self", links) + "/attachments";
		}
		
		else {
			try {
	
				for(Link link: links) {
					if(link.getRel().equals(rel)) {
						href = link.getHref();
						// No need to search more if found
						break;
					} 
				}
			} catch(Exception e) {
				logger.warn("Exception while retrieving 'href' from link {}. Error Message: {}",rel,  e.getMessage());
			}
		}
		// TODO : Tweak : Should be properly fixed on the wiki side (???). Or from conf, a "forceScheme"
		
		href = href.replace("http://", "https://");
		return href;
	}
	
}
