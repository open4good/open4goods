package org.open4goods.xwiki.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.open4goods.xwiki.config.UrlManagementHelper;
import org.open4goods.xwiki.config.XWikiConstantsRelations;
import org.open4goods.xwiki.config.XWikiServiceProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import org.xwiki.rest.model.jaxb.Attachment;
import org.xwiki.rest.model.jaxb.Attachments;
import org.xwiki.rest.model.jaxb.ObjectSummary;
import org.xwiki.rest.model.jaxb.Objects;
import org.xwiki.rest.model.jaxb.Page;
import org.xwiki.rest.model.jaxb.Pages;
import org.xwiki.rest.model.jaxb.Properties;
import org.xwiki.rest.model.jaxb.Property;
import org.xwiki.rest.model.jaxb.SearchResult;
import org.xwiki.rest.model.jaxb.SearchResults;
import org.xwiki.rest.model.jaxb.Wiki;
import org.xwiki.rest.model.jaxb.Wikis;
import org.xwiki.rest.model.jaxb.Xwiki;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;

import io.micrometer.common.util.StringUtils;

/**
 * Services related to XWiki object mapping 
 * 
 * @author Thierry.Ledan
 *
 */
public class XwikiMappingService {

	private static Logger logger = LoggerFactory.getLogger(XwikiMappingService.class);

	RestTemplateService restTemplateService;
	XWikiServiceProperties properties;
	private UrlManagementHelper urlHelper;
	
	public XwikiMappingService(RestTemplateService restTemplateService, XWikiServiceProperties properties){
		this.restTemplateService = restTemplateService;
		this.properties = properties;
		this.urlHelper = new UrlManagementHelper(properties);
	}
	
	/**
	 * Map 'Page' object from json response endpoint
	 * @param endpoint
	 * @return a 'Page' object if response and mapping were successful, null otherwise
	 * @throws ResponseStatusException
	 */
	public Page mapPage(String endpoint) throws ResponseStatusException {

		Page page = null;
		// get response from rest server
		ResponseEntity<String> response = restTemplateService.getRestResponse(endpoint);
		if (response != null ) {
			page = deserializePage(response);
		} 
		return page;
	}
	
	/**
	 * Map 'Pages' object from json response endpoint
	 * @param String endpoint 
	 * @return a 'Pages' object if response and mapping were successful, null otherwise
	 */
	public Pages mapPages(String endpoint) {

		Pages pages = null;
		// get response from rest server
		ResponseEntity<String> response = restTemplateService.getRestResponse(endpoint);
		if (response != null ) {
			pages = deserializePages(response);
		} 
		return pages;
	}
	
	
	/**
	 * Map 'SearchResults' object from json response endpoint
	 * @param String endpoint 
	 * @return a 'SearchResults' object if response and mapping were successful, null otherwise
	 */
	public SearchResults mapSearchResults(String endpoint) {

		SearchResults results = null;
		ResponseEntity<String> response = restTemplateService.getRestResponse(endpoint);
		if( response != null ) {
			results = deserializeSearchResults(response);
		}
		return results;
	}
	
	
	/**
	 * Get properties related to 'page'
	 * To find out the good url to properties, it's a tricky method:
	 * First get the 'Objects' from 'Page' that contains a list of 'ObjectSummary'.
	 * Pick up the first one (!!!!) and get the href link from the 'properties' rel link
	 * 
	 * @param page
	 * @return page's properties
	 */
	public Map<String, String> getProperties(Page page){

		Map<String, String> properties = new HashMap<String, String>();
		Properties propertiesObject = new Properties();
		Objects objects = page.getObjects();
		if( objects == null ) {
			// request to server 
			objects = getPageObjects(page);
		} 
		if( objects != null ) {
			// TODO: hard coding - objects.getObjectSummaries().get(0).getLinks() NOT GOOD , try to find the good summary !!
			String pagePropsEndpoint = urlHelper.getHref(XWikiConstantsRelations.REL_PROPERTIES, objects.getObjectSummaries().get(0).getLinks());

			ResponseEntity<String> response = restTemplateService.getRestResponse(pagePropsEndpoint);
			if( response != null ) {
				propertiesObject = deserializeProperties(response);
				if( propertiesObject != null ) {
					for(Property prop: propertiesObject.getProperties()) {
						properties.put(prop.getName(), prop.getValue());
					}
				}
			}
		}
		return properties;
	}
		
		
	/**
	 *
	 * Get properties related to an object of class 'className' in the ObjectSummary list
	 * 
	 * @param page
	 * @return page's properties
	 */
	public Map<String, String> getUserProperties(Objects objects, String className){

		Map<String, String> properties = new HashMap<String, String>();
		Properties propertiesObject = new Properties();

		String propertiesEndpoint = null;
		if( objects != null && objects.getObjectSummaries() != null && ! objects.getObjectSummaries().isEmpty() ) {
			for( ObjectSummary object: objects.getObjectSummaries() ) {
				if( object.getClass().equals(className) ) {
					propertiesEndpoint = urlHelper.getHref(XWikiConstantsRelations.REL_PROPERTY, object.getLinks());
					break;
				}
			}
		}
		// fetch properties
		if( StringUtils.isNotBlank(propertiesEndpoint) ) {
			ResponseEntity<String> response = restTemplateService.getRestResponse(propertiesEndpoint);
			if( response != null ) {
				propertiesObject = deserializeProperties(response);
				if( propertiesObject != null ) {
					for(Property prop: propertiesObject.getProperties()) {
						properties.put(prop.getName(), prop.getValue());
					}
				}
			}
		}
		return properties;
	}
	
	
	/**
	 * fetch value for page's field 'objects'
	 * @param page
	 * @return
	 */
	public Objects getPageObjects(Page page) {

		String objectsUrl = urlHelper.getHref(XWikiConstantsRelations.REL_OBJECTS, page.getLinks());
		return getObjects(objectsUrl);
	}
	
	
	/**
	 * fetch value for page's field 'objects'
	 * @param page
	 * @return
	 */
	public Objects getObjects(String pageEndpoint) {

		Objects objects = null;
		if( StringUtils.isNotEmpty(pageEndpoint) ) {
			ResponseEntity<String> response = restTemplateService.getRestResponse(pageEndpoint);
			if( response != null ) {
				objects = deserializeObjects(response);
			}
		}
		return objects;
	}
	
	
	/**
	 * Get page attachments
	 * 
	 * @param page targeted Page
	 * @return Attachments object
	 */
	public Attachments getAttachments (Page page) {

		Attachments attachments = null;
		// get the attachments url in 'links' set
		String attachementsUrl = urlHelper.getHref(XWikiConstantsRelations.REL_ATTACHMENTS, page.getLinks());
		if( StringUtils.isNotBlank(attachementsUrl) ) {
			ResponseEntity<String> response = null;
			response = restTemplateService.getRestResponse(attachementsUrl);
			if( response != null ) {
				attachments = deserializeAttachments(response);
			}
		}
		return attachments;
	}
	
	
	/**
	 * Retrieve attachments associated to a Page
	 * @param response
	 * @return the Attachments list related to Page, empty list if no attachment
	 */
	public List<Attachment> getAttachmentList (Page page) {

		Attachments attachments;
		List<Attachment> attachmentsList = new ArrayList<Attachment>();
		// get the url
		String attachementsUrl = urlHelper.getHref(XWikiConstantsRelations.REL_ATTACHMENTS, page.getLinks());
		if( StringUtils.isNotBlank(attachementsUrl) ) {
			ResponseEntity<String> response = null;
			response = restTemplateService.getRestResponse(attachementsUrl);
			if( response != null ) {
				attachments = deserializeAttachments(response);
				if( attachments != null ) {
					attachmentsList = attachments.getAttachments();
				}
			}
		}
		return attachmentsList;
	}
	
	
	/**
	 * return attachment url (../bin/download/....)
	 * @param attachment
	 * @return
	 */
//	public String getAttachmentUrl(Attachment attachment) {
//
//		String absoluteUrl = attachment.getXwikiAbsoluteUrl();
//		return restTemplateService.updateUrlScheme(absoluteUrl);	 
//	}

	
	/**
	 * 
	 * @param url
	 * TODO : Should provide a streamed version. See IcecatFeatureService.downloadTo()
	 * @return
	 */
	public byte[] downloadAttachment( String url ) {
		byte[] blob = null;
		ResponseEntity<byte[]> response = restTemplateService.downloadAttachment(url);
		if( response != null ) {
			blob = response.getBody();
		}
		return blob;
	}
	
	
	
	/**
	 * Retrieve available wikis
	 * @param endpoint
	 * @return
	 */
	public List<Wiki> getAllWikis( String endpoint ){

		List<Wiki> wikisList = new ArrayList<Wiki>();
		ResponseEntity<String> response = restTemplateService.getRestResponse(endpoint);
		if(response != null) {
			Wikis wikis = deserializeWikis(response);
			if(wikis != null) {
				wikisList = wikis.getWikis();
			}
		}
		return wikisList;
	}
	
	
	
	
	
	
	

	/**
	 * Return true if 'wiki' exists in available wikis list
	 * @param wiki
	 * @return
	 */
//	public boolean checkWikiExists(String targetWiki) {
//
//		boolean exists = false;
//		Xwiki xwiki = null;
//		String wikisHref = null;
//		// get response from rest server
//		String endpoint = resourcesPathManager.getApiEntryPoint();
//		ResponseEntity<String> response = restTemplateService.getRestResponse(endpoint);
//		if (response != null ) {
//			xwiki = deserializeXwiki(response);
//			if(xwiki != null) {
//				for(Link link: xwiki.getLinks()) {
//					if(link.getRel().equals(XWikiConstantsRelations.REL_WIKIS)) {
//						wikisHref = link.getHref();
//						break;
//					}
//				}
//				// get available wikis
//				List<Wiki> wikis = getAllWikis(wikisHref);
//				if(wikis != null) {
//					for(Wiki wiki: wikis) {
//						if(wiki.getName().equals(targetWiki)) {
//							exists = true;
//							break;
//						}
//					}
//				}
//			}
//		} 
//		return exists;
//	}
	
	
	
	
	
	//-----------------------------------------------------------
	// DESERIALIZATION MANAGEMENT
	//-----------------------------------------------------------

	/**
	 * Deserialize json response to 'Objects' object
	 * @param response
	 * @return a 'Attachments' object if the mapping was successful, null otherwise
	 */
	private Objects deserializeObjects(ResponseEntity<String> response) {

		Objects objects = null;
		try {
			ObjectMapper mapper = new ObjectMapper();
			objects = mapper.readValue(response.getBody(),new TypeReference<Objects>(){});
			logger.debug("Object 'Objects' mapped correctly}");
		}
		catch(Exception e) {
			ManageMappingExceptions(e, "Objects", response.getBody());
		}
		return objects;
	}
	
	/**
	 * Deserialize json response to 'Attachments' object
	 * @param response
	 * @return a 'Attachments' object if the mapping was successful, null otherwise
	 */
	private Attachments deserializeAttachments(ResponseEntity<String> response) {

		Attachments attachments = null;
		try {
			ObjectMapper mapper = new ObjectMapper();
			attachments = mapper.readValue(response.getBody(),new TypeReference<Attachments>(){});
			logger.debug("Object 'Attachments' mapped correctly}");
		}
		catch(Exception e) {
			ManageMappingExceptions(e, "Attachments", response.getBody());
		}
		return attachments;
	}
	
	
	/**
	 * Deserialize json response to 'Page' object
	 * @param response
	 * @return a 'Page' object if the mapping was successful, null otherwise
	 */
	private Page deserializePage(ResponseEntity<String> response) {

		Page page = null;
		try {
			// TODO(p1,perf) : Do not re instanciate
			ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
			page = mapper.readValue(response.getBody(),new TypeReference<Page>(){});
			logger.debug("Object 'Page' mapped correctly}");
		}
		catch(Exception e) {
			ManageMappingExceptions(e, "Page", response.getBody());
		}
		return page;
	}
	
	/**
	 * Deserialize json response to 'SearchResults' object
	 * @param response
	 * @return a 'SearchResults' object if the mapping was successful, null otherwise
	 */
	public SearchResults deserializeSearchResults(ResponseEntity<String> response) {

		SearchResults results = null;	
		try {
			ObjectMapper mapper = new ObjectMapper();
			results = mapper.readValue(response.getBody(), new TypeReference<SearchResults>() {});
		} catch( Exception e ) {
			ManageMappingExceptions(e, "SearchResults", response.getBody());
		}
		return results;
	}

	/**
	 * Deserialize json response to 'SearchResult' object
	 * @param response
	 * @return a 'SearchResult' object if the mapping was successful, null otherwise
	 */
	public SearchResult deserializeSearchResult(ResponseEntity<String> response) {

		SearchResult result = null;
		try {
			ObjectMapper mapper = new ObjectMapper();
			result = mapper.readValue(response.getBody(), new TypeReference<SearchResult>() {});
		} catch( Exception e ) {
			ManageMappingExceptions(e, "SearchResult", response.getBody());
		}
		return result;
	}

	/**
	 * Deserialize json response to 'Pages' object
	 * @param response
	 * @return a 'Pages' object if the mapping was successful, null otherwise
	 */
	public Pages deserializePages(ResponseEntity<String> response) {

		Pages pages = null;
		try {
			ObjectMapper mapper = new ObjectMapper();
			pages = mapper.readValue(response.getBody(),new TypeReference<Pages>(){});
			logger.debug("Object 'Pages' mapped correctly}");
		}
		catch(Exception e) {
			ManageMappingExceptions(e, "Pages", response.getBody());
		}
		return pages;
	}
	
	/**
	 * Deserialize json response to 'Properties' object
	 * @param response
	 * @return a 'Properties' object if the mapping was successful, null otherwise
	 */
	private Properties deserializeProperties(ResponseEntity<String> response) {		

		Properties properties = null;
		if( response != null ) {
			try {
				ObjectMapper mapper = new ObjectMapper();
				properties = mapper.readValue(response.getBody(),new TypeReference<Properties>(){});
				logger.debug("Object 'Map<String,String>' mapped correctly}");
			}
			catch(Exception e) {
				ManageMappingExceptions(e, "Map<String,String>", response.getBody());
			}
		}
		return properties;	
	}

	/**
	 * Deserialize json response to 'Xwiki' object
	 * @param response
	 * @return a 'Xwiki' object if the mapping was successful, null otherwise
	 */
	public Xwiki deserializeXwiki(ResponseEntity<String> response) {

		Xwiki xWiki = null;
		try {
			ObjectMapper mapper = new ObjectMapper();
			xWiki = mapper.readValue(response.getBody(),new TypeReference<Xwiki>(){});
			logger.debug("Object 'Xwiki' mapped correctly}");
		}	
		catch(Exception e) {
			ManageMappingExceptions(e, "Xwiki", response.getBody());
		}
		return xWiki;
	}

	/**
	 * Deserialize json response to 'Wikis' object
	 * @param response
	 * @return a 'Wikis' object if the mapping was successful, null otherwise
	 */
	public Wikis deserializeWikis(ResponseEntity<String> response) {

		Wikis wikis = null;
		try {
			ObjectMapper mapper = new ObjectMapper();
			wikis = mapper.readValue(response.getBody(),new TypeReference<Wikis>(){});
			logger.debug("Object 'Wikis' mapped correctly}");
		}
		catch(Exception e) {
			ManageMappingExceptions(e, "Wikis", response.getBody());
		}
		return wikis;
	}

	/**
	 * Deserialize json response to 'Wiki' object
	 * @param response
	 * @return a 'Wiki' object if the mapping was successful, null otherwise
	 */
	public Wiki deserializeWiki(ResponseEntity<String> response) {

		Wiki wikis = null;
		try {
			ObjectMapper mapper = new ObjectMapper();
			wikis = mapper.readValue(response.getBody(),new TypeReference<Wiki>(){});
			logger.debug("Object 'Wiki' mapped correctly}");
		}
		catch(Exception e) {
			ManageMappingExceptions(e, "Wiki", response.getBody());
		}
		return wikis;
	}

	

	//-----------------------------------------------------------
	// EXCEPTION MANAGEMENT
	//----------------------------------------------------------
	
	/**
	 * Manage error messages depending on exception type
	 * @param e the exception
	 * @param typeToMap the Type to Map
	 * @param responseBody rest response body
	 */
	private void ManageMappingExceptions(Exception e, String typeToMap, String responseBody) {

		if(e instanceof UnrecognizedPropertyException) {
			logger.warn("Unable to map '{}' object from json: Unrecognized property:'{}' in Object:'{}'. Known properties:'{}'", 
					typeToMap,
					((UnrecognizedPropertyException)e).getPropertyName(),
					((UnrecognizedPropertyException)e).getReferringClass().toString(), 
					((UnrecognizedPropertyException)e).getKnownPropertyIds().toString());
		} else {
			logger.warn("Unable to map '{}' object from json:{}", 
					typeToMap,
					responseBody);
		}
		throw new ResponseStatusException(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build().getStatusCode(), "Unable to map " + typeToMap + "' object from json response.");
	}
}
