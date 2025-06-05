package org.open4goods.xwiki.services;

import java.util.Base64;

import org.open4goods.xwiki.config.UrlManagementHelper;
import org.open4goods.xwiki.config.XWikiConstantsResourcesPath;
import org.open4goods.xwiki.config.XWikiServiceProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

/**
 * Http services to request the XWiki server
 * 
 * @author Thierry.Ledan
 *
 */
public class RestTemplateService {


	private RestTemplate restTemplate;
	private RestTemplate webTemplate;
	private XWikiServiceProperties properties;
	private XWikiConstantsResourcesPath resourcesPathManager;
	private UrlManagementHelper urlHelper;;
	
	private static Logger logger = LoggerFactory.getLogger(RestTemplateService.class);
	
	public RestTemplateService(RestTemplate restTemplate, RestTemplate webTemplate, XWikiServiceProperties properties) {
		this.restTemplate = restTemplate;
		this.webTemplate = webTemplate;
		this.properties = properties;
		this.urlHelper = new UrlManagementHelper(properties);
		//this.resourcesPathManager = new XWikiConstantsResourcesPath(this.properties.getBaseUrl(), this.properties.getApiEntrypoint(), this.properties.getApiWiki());
	}
	
	/**
	 * Return Response from a REST service endpoint if status code equals to 2xx
	 * Null otherwise (exception, status code not equals to 2xxx)
	 * @param endpoint
	 * @return Response if status code equals to 2xxx, null otherwise
	 */
	public  ResponseEntity<String> getRestResponse ( String endpoint ) throws ResponseStatusException {

		ResponseEntity<String> response = null;
		String updatedEndpoint = null;
		
		if(endpoint != null) {
			try {
				// first clean url: url decoding, check scheme and add query params if needed
				updatedEndpoint = urlHelper.cleanUrl(endpoint);
				logger.info("request xwiki server with endpoint {}", updatedEndpoint);
				
				HttpHeaders headers = authenticatedHeaders();	
				
				HttpEntity<String> request = new HttpEntity<String>(headers);
				response = restTemplate.exchange(updatedEndpoint, HttpMethod.GET, request, String.class);
			} catch(RestClientResponseException rcre) {
				logger.warn("HttpClientErrorException exception  - uri:{} ", updatedEndpoint, rcre);
				throw new ResponseStatusException(rcre.getStatusCode(),rcre.getResponseBodyAsString());
			} catch(Exception e) {
				logger.warn("Exception while trying to reach endpoint:{}", updatedEndpoint, e);
				throw new ResponseStatusException(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build().getStatusCode(), e.getMessage());
			}
		}
		return response;
	}
	

	/**
	 * 
	 * @param viewUrl
	 * @return
	 */
	
	public ResponseEntity<String> getWebResponse( String xwikiWebUrl ){

		ResponseEntity<String> response = null;
		logger.info("request xwiki web server with url {}", xwikiWebUrl);
		if(xwikiWebUrl != null) {
			try {
				HttpHeaders headers = authenticatedHeaders();							
				HttpEntity<String> request = new HttpEntity<String>(headers);
				response = restTemplate.exchange(xwikiWebUrl, HttpMethod.GET, request, String.class);
			} catch(Exception e) {
				logger.error("Exception while trying to reach url:{} - error:{}", xwikiWebUrl, e.getMessage());
			}
			// check response status code
			if (null != response && ! response.getStatusCode().is2xxSuccessful()) {
				logger.warn("Response returns with status code:{} - for uri:{}", response.getStatusCode(), xwikiWebUrl);
				response = null;
			} 
		}
		return response;
	}
	
	/**
	 * 
	 * @param url
	 * @return
	 */
	public ResponseEntity<byte[]> downloadAttachment(String url) {
		
		ResponseEntity<byte[]> response = null;
		if(url != null) {
			try {
				
				HttpHeaders headers = authenticatedHeaders();							
				HttpEntity<String> request = new HttpEntity<String>(headers);
				response = restTemplate.exchange(url, HttpMethod.GET, request, byte[].class);
			
			} catch(Exception e) {
				logger.error("Exception while trying to reach url:{} - error:{}", url, e.getMessage());
			}
			// check response status code
			if (null != response && ! response.getStatusCode().is2xxSuccessful()) {
				logger.warn("Response returns with status code:{} - for uri:{}", response.getStatusCode(), url);
				response = null;
			} 
		}
		
		return response;
		
	}
	
	
//	/**
//	 * Clean url: 
//	 * 				URLDecoding  
//	 * 				Force https if httpsOnly set
//	 * 				Add 'media' query param from properties
//	 * @param url
//	 * @param key
//	 * @param value
//	 * @return this url with query param key=value if process succedded, null otherwise
//	 */
//	public String cleanUrl(String url) {
//
//		String clean = "";
//		try {
//			if( url != null ) {
//				String secureUrl = updateUrlScheme(url);
//				String decoded = URLDecoder.decode(secureUrl, Charset.defaultCharset());
//				// add request param if needed
//				clean = addQueryParam(decoded, "media", this.properties.getMedia());
//			}
//		} catch (Exception e) {
//			logger.warn("Exception while updating url {}. Error Message: {}",url,  e.getMessage());
//		}
//		return clean;
//	}


//	/**
//	 * Update http scheme in https if needed
//	 * @param url
//	 * @return
//	 */
//	public String updateUrlScheme(String url) {
//
//		String updated = url;
//		if(url != null && this.properties.isHttpsOnly()) {
//			updated = url.replaceFirst("http:", "https:");
//		}
//		return updated;
//	}

//	/**
//	 * Add query params to url
//	 * @param url
//	 * @param key
//	 * @param value
//	 * @return this url with query param key=value if process succedded, null otherwise
//	 */
//	public String addQueryParam(String url, String key, String value) {
//
//		String uriWithParams = null;
//		try{
//			URI uri = UriComponentsBuilder.fromUriString(url)
//					.queryParam(key, value)
//					.build()
//					.toUri();
//
//			if(uri != null) {
//				// decode again after adding query params
//				uriWithParams = URLDecoder.decode(uri.toString(), Charset.defaultCharset());
//			}
//
//		} catch(Exception e) {
//			logger.warn("Unable to add query params {}={} to uri {}. Error Message:{}",key, value, url, e.getMessage());
//		}       
//		return uriWithParams;
//
//	}
	
	
	/**
	 * Retrieve http headers that will allow to authenticate against the wiki
	 * @param user
	 * @param password
	 * @return
	 */
	public HttpHeaders authenticatedHeaders(String user, String password) {
		String plainCreds = user +":"+password;
		byte[] plainCredsBytes = plainCreds.getBytes();
		byte[] base64CredsBytes = Base64.getEncoder().encode(plainCredsBytes);
		String base64Creds = new String(base64CredsBytes);

		HttpHeaders headers = new HttpHeaders();
		headers.add("Authorization", "Basic " + base64Creds);
		return headers;
	}

	/**
	 * Retrieve http headers that will allow to authenticate against the wiki, using the configuration
	 * @param config
	 * @return
	 */
	public HttpHeaders authenticatedHeaders() {
		return authenticatedHeaders(properties.getUsername(), properties.getPassword());
	}

	public RestTemplate getRestTemplate() {
		return restTemplate;
	}

	public void setRestTemplate(RestTemplate restTemplate) {
		this.restTemplate = restTemplate;
	}

	public RestTemplate getWebTemplate() {
		return webTemplate;
	}

	public void setWebTemplate(RestTemplate webTemplate) {
		this.webTemplate = webTemplate;
	}

	
}
