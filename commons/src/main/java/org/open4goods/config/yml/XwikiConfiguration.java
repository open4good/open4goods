package org.open4goods.config.yml;

import java.net.URISyntaxException;

/**
 * Configuration for an XWiki connexion
 * @author goulven
 *
 */
public class XwikiConfiguration {

	private static final String XWIKI_VIEW_PATH = "bin/view/";

	private static final String XWIKI_GROUPES_URL = "bin/view/Main/listeGroupes/";

	
    private String user ;
    private String password ;
    private String baseUrl ;
    
    /**
     * 
     * @return
     * @throws URISyntaxException 
     */
    public String groupsUrl() {
		return (baseUrl + "/"+XWIKI_GROUPES_URL);
	}


	public String viewPath() {		
		return baseUrl +"/"+ XWIKI_VIEW_PATH;
	}
		
	/**
	 * Return the edit url for a vertical (space) and a page
	 * @param vertical
	 * @param page
	 * @return
	 */
	public String getEditUrl(String vertical, String page) {		
		return getBaseUrl() + "/bin/edit/" + vertical + "/" + page+"?template=XWiki.PageTemplate";
	}
		
    
	public String getUser() {
		return user;
	}
	public void setUser(final String xwikiUser) {
		this.user = xwikiUser;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(final String xwikiPassword) {
		this.password = xwikiPassword;
	}
	public String getBaseUrl() {
		return baseUrl;
	}
	public void setBaseUrl(final String wikiBaseUrl) {
		this.baseUrl = wikiBaseUrl;
	}





}