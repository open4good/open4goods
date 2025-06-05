package org.open4goods.xwiki.config;

import org.apache.commons.lang3.StringUtils;

/**
 * handles path for endpoints
 * 
 * @author Thierry.Ledan
 * TODO : In properties class, or static (or both !)
 */
public class XWikiConstantsResourcesPath {

	// REST CONSTANTS
	private static final String WIKIS_PATH = "wikis";
	private static final String SPACES_PATH = "spaces";
	private static final String PAGES_PATH = "pages";
	private static final String OBJECTS_PATH = "objects";
	private static final String URI_SEPARATOR = "/";
	
	private static final String ADMIN_SPACE = "XWiki";
	
	// classes
	private static final String USERS_CLASNAME = "XWiki.XWikiUsers";
	private static final String GROUPS_CLASNAME = "XWiki.XWikiGroups";
	//public static final String RIGHTS_CLASNAME = "XWiki.XWikiRights";
	// query
	private static final String QUERY_USERS = "query?q=object:" + USERS_CLASNAME;
	private static final String QUERY_GROUPS = "query?q=object:" + GROUPS_CLASNAME;	
	
	
	// VIEW CONSTANTS 
	private static final String VIEW_PATH = "bin/view";
	// EDIT CONSTANTS 
	private static final String EDIT_PATH = "bin/edit";
		
	// download 
	private static final String DOWNLOAD_PATH = "bin/download";
	
	// login
	private static final String CURRENT_USER_GROUPS_PATH = "testGroups/currentUserGroups";

	// 	private static final String CURRENT_USER_GROUPS_PATH = "https://wiki.nudger.fr/bin/view/testGroups/currentUserGroups";

	
	private String baseUrl;
	private String apiEntryPoint;
	private String wikiName;

	
	/**
	 * 
	 * @param baseUrl
	 * @param apiEntryPoint
	 * @param wikiName
	 */
	public XWikiConstantsResourcesPath(String baseUrl, String apiEntryPoint, String wikiName) {
		this.baseUrl = baseUrl;
		this.apiEntryPoint = apiEntryPoint;
		this.wikiName = wikiName;
	}
	
	public String getBaseUrl() {
		return this.baseUrl;
	}
	
	public String getWikiName() {
		return this.wikiName;
	}
	
	/**
	 * returns the rest api entry point (just before /{wikis/wikiname})
	 * @return
	 */
	public String getApiEntryPoint() {
		return 	getBaseUrl() +
				URI_SEPARATOR +
				this.apiEntryPoint;
	}
	
	/**
	 * returns the path to get xwiki web page
	 * @return
	 */
	public String getViewpath() {
		return getBaseUrl() +
				URI_SEPARATOR +
				VIEW_PATH +
				URI_SEPARATOR;
	}
	
	/**
	 * returns the path to get xwiki web page
	 * @return
	 */
	public String getEditpath() {
		return getBaseUrl() +
				URI_SEPARATOR +
				EDIT_PATH +
				URI_SEPARATOR;
	}
	
	/**
	 * returns the path to get xwiki web page
	 * @return
	 */
	public String getEditpath(String... path) {
		return getEditpath() + StringUtils.join(path,"/");
	}
	
	/**
	 * returns the path to get xwiki web page
	 * @return
	 */
	public String getDownloadpath() {
		return getBaseUrl() +
				URI_SEPARATOR +
				DOWNLOAD_PATH+
				URI_SEPARATOR;
	}
	
	/**
	 * returns the path to get xwiki web page
	 * @return
	 */
	public String getDownloadAttachlmentUrl(String space, String name, String attachmentName) {
		return getBaseUrl() +
				URI_SEPARATOR +
				DOWNLOAD_PATH+
				URI_SEPARATOR +
				space +
				URI_SEPARATOR +
				name + 
				URI_SEPARATOR +
				attachmentName;
				
	}
	
	/**
	 * Return endpoint to wiki
	 * returned path ends with '/' 
	 * @return
	 */
	public String getWikisEndpoint() {
		return getApiEntryPoint() +
				URI_SEPARATOR +
				WIKIS_PATH +
				URI_SEPARATOR +
				getWikiName() +
				URI_SEPARATOR;
	}

	/**
	 * path to 'space'. 
	 * returned path ends with '/' 
	 * @param space
	 * @return
	 */
	public String getSpacesEndpoint() {
		return getWikisEndpoint() +
				SPACES_PATH +
				URI_SEPARATOR;
	}
	
	/**
	 * path to 'space'. 
	 * returned path ends with '/' 
	 * @param space
	 * @return
	 */
	public String getSpacesEndpoint( String spacesPath ) {
		return getSpacesEndpoint() +
				spacesPath +
				URI_SEPARATOR;
	}
	

	/**
	 * path to 'page' related to 'space'. 
	 * returned path ends with '/' 
	 * @param space
	 * @param page
	 * @return
	 */
	public String getPagesEndpoint( String spacesPath ) {
		return getSpacesEndpoint(spacesPath) +
				PAGES_PATH +
				URI_SEPARATOR ;
	}
	
	/**
	 * path to 'page' related to 'space'. 
	 * returned path ends with '/' 
	 * @param space
	 * @param page
	 * @return
	 */
	public String getPageEndpoint( String spacesPath, String page ) {
		return getPagesEndpoint(spacesPath) +
				page +
				URI_SEPARATOR;
	}
	

	public String getGroupsEndpoint() {
		return getApiEntryPoint() +
				URI_SEPARATOR +
				WIKIS_PATH +
				URI_SEPARATOR +
				QUERY_GROUPS;
	}
	
	public String getGroupUsers(String groupPageName) {
		return getPageEndpoint(ADMIN_SPACE, groupPageName) + OBJECTS_PATH;
	}
	
	public String getUsersEndpoint() {
		return getApiEntryPoint() +
				URI_SEPARATOR +
				WIKIS_PATH +
				QUERY_USERS;
	}
	
	public String getUserEndpoint( String userName ) {
		return getPageEndpoint(ADMIN_SPACE, userName);
	}
	
	
	/**
	 * return the endpoint to query all groups
	 * @return
	 */
	public String getQueryGroupsEndpoint() {
		return QUERY_GROUPS;
	}
	
	/**
	 * return the endpoint to query all users
	 * @return
	 */
	public String getQueryUsersEndpoint() {
		return QUERY_USERS;
	}
	
	/**
	 * return the xwiki Users classname's value 
	 * @return
	 */
	public String getUsersClassName() {
		return USERS_CLASNAME;
	}
	
	/**
	 * return the xwiki Groups classname's value 
	 * @return
	 */
	public String getGroupsClassName() {
		return GROUPS_CLASNAME;
	}
	
	
	/**
	 * Endpoint to retrieve logged user's groups
	 * @return
	 */
	public String getCurrentUserGroupsEndpoint() {
		return 	getBaseUrl() +
				URI_SEPARATOR +
				VIEW_PATH +
				URI_SEPARATOR +
				CURRENT_USER_GROUPS_PATH;
	}
}
