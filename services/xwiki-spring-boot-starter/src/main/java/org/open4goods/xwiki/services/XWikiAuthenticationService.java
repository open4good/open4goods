package org.open4goods.xwiki.services;

import java.util.Arrays;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.open4goods.xwiki.XWikiServiceConfiguration;
import org.open4goods.xwiki.config.UrlManagementHelper;
import org.open4goods.xwiki.config.XWikiConstantsResourcesPath;
import org.open4goods.xwiki.config.XWikiServiceProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.UnknownHttpStatusCodeException;

/**
 * Services related to authentication
 * 
 * @author Thierry.Ledan
 *
 */
public class XWikiAuthenticationService {

	private static Logger logger = LoggerFactory.getLogger(RestTemplateService.class);

	private XWikiServiceProperties xWikiProperties;
	private XWikiConstantsResourcesPath resourcesPathManager;
	private XwikiMappingService mappingService;
	private RestTemplateBuilder loginRestTemplateBuilder;
	private RestTemplateService restTemplateService;
	private UrlManagementHelper urlHelper;
	
	public XWikiAuthenticationService (XwikiMappingService mappingService,  RestTemplateService restTemplateService, XWikiServiceProperties xWikiProperties, RestTemplateBuilder restTemplateBuilder) throws Exception {
		this.xWikiProperties = xWikiProperties;
		this.restTemplateService = restTemplateService;
		this.mappingService = mappingService;
		this.resourcesPathManager = new XWikiConstantsResourcesPath(xWikiProperties.getBaseUrl(), xWikiProperties.getApiEntrypoint(), xWikiProperties.getApiWiki());
		this.loginRestTemplateBuilder = restTemplateBuilder;
		this.urlHelper = new UrlManagementHelper(xWikiProperties);
	}
	
	
	/**
	 * Login on xwiki and return groups belonging to the current user
	 * 
	 * @param userName current username
	 * @param password
	 * @return List of groups belonging to the current user
	 * @throws Exception
	 */
	@Cacheable(cacheNames = XWikiServiceConfiguration.ONE_HOUR_LOCAL_CACHE_NAME, key = "#root.methodName + ':' + #userName +  ':' + #password")
	public List<String> login( String userName, String password) throws Exception {
		
		List<String> groups = null;
		String endpoint = resourcesPathManager.getCurrentUserGroupsEndpoint();
		
		RestTemplate loginRestTemplate =  loginRestTemplateBuilder.basicAuthentication(userName, password).build();
		ResponseEntity<String> response = null;

		// first clean url: url decoding, check scheme and add query params if needed
		String updatedEndpoint = this.urlHelper.cleanUrl(endpoint);
		logger.info("request xwiki server with endpoint {}", updatedEndpoint);

		if(updatedEndpoint != null) {
			try {
				response = loginRestTemplate.getForEntity(updatedEndpoint, String.class);
			} 
			// HTTP status 4xx
			catch(HttpClientErrorException e) {
				logger.warn("Client error - uri:{} - error:{}", updatedEndpoint, e.getStatusCode().toString());
				throw new Exception(e.getStatusText());
			} 
			// HTTP status 5xx
			catch(HttpServerErrorException e) {
				logger.warn("Server error - uri:{} - error:{}", updatedEndpoint, e.getStatusCode().toString());
				throw new Exception(e.getStatusText());
			} 
			//  unknown HTTP status
			catch(UnknownHttpStatusCodeException e) {
				logger.warn("Server error response  - uri:{} - error:{}", updatedEndpoint, e.getStatusCode().toString());
				throw new Exception("Login error");
			} 
			// other errors
			catch(Exception e) {
				logger.warn("Exception while trying to reach endpoint:{} - error:{}", updatedEndpoint, e.getMessage());
				throw new Exception("Login error");
			}
			
			// check response status code
			if (null != response && response.getStatusCode().is2xxSuccessful()) {
				try {
					// parse and get groups
					Document doc = Jsoup.parse(response.getBody());
					Element div = doc.getElementById("xwikicontent");
					String content = div.text();					
					logger.debug("Groups retrieved in html:" + content);
					groups = Arrays.asList(content.substring(1, content.length() - 1).split(","));
					
				} catch( Exception e ) {
					// TODO: how to manage this error: login succeedded but parsing error !!
					logger.warn("Exception while searching groups in html response: {}", response.getBody());
					throw new Exception("Groups parsing error");
				}
			} else {
				logger.warn("Response returns with status code:{} - for uri:{}", response.getStatusCode(), updatedEndpoint);
				response = null;
			}
		}
		return groups;
	}
}
