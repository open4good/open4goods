package org.open4goods.services;


import java.io.File;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.open4goods.config.yml.XwikiConfiguration;
import org.open4goods.exceptions.InvalidParameterException;
import org.open4goods.exceptions.ResourceNotFoundException;
import org.open4goods.exceptions.TechnicalException;
import org.open4goods.helper.DocumentHelper;
import org.open4goods.helper.XpathHelper;
import org.open4goods.model.dto.WikiResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * This service handles XWiki auth and xwiki content retrieving
 *
 * @author Goulven.Furet 
 */
public class XwikiService {
	
	private static final Logger logger = LoggerFactory.getLogger(XwikiService.class);
		
	private final Map<String,WikiResult> contentCache = new ConcurrentHashMap<>();
	
	private static final String GROUPS_MARKUP_START = "%GROUPES%";
	private static final String GROUPS_MARKUP_END = "%/GROUPES%";
	
	private static String MARKER = "<div id=\"xwikicontent\" class=\"col-xs-12\">";
	
	private final XwikiConfiguration config;

	private final RestTemplate restTemplate = new RestTemplate();

	
	public XwikiService(XwikiConfiguration config) {
		this.config = config;
	}
	
	/**
	 * Try to login against the wiki, return the groups if succeed. The XWIKI_GROUPES_URL page must be :
	 * 
	 * 
	{{velocity}}
	#set($allGroupsInAllWikis = $services.user.group.getGroupsFromAllWikis($xcontext.userReference))
	%GROUPES%$allGroupsInAllWikis%/GROUPES%
{{/velocity}}
	 * 
	 * @param user
	 * @param password
	 * @return
	 * @throws TechnicalException
	 * @throws InvalidParameterException
	 */
	public List<String> loginAndGetGroups (String user, String password) throws TechnicalException, InvalidParameterException{
		
		
		String plainCreds = user +":"+password;
		byte[] plainCredsBytes = plainCreds.getBytes();
		byte[] base64CredsBytes = Base64.encodeBase64(plainCredsBytes);
		String base64Creds = new String(base64CredsBytes);

		HttpHeaders headers = new HttpHeaders();
		headers.add("Authorization", "Basic " + base64Creds);
		
		ResponseEntity<String> response = null;
		try {
		
			HttpEntity<String> request = new HttpEntity<String>(headers);
			response = restTemplate.exchange(config.groupsUrl(), HttpMethod.GET, request, String.class);
		} catch (Exception e) {
			throw new TechnicalException("Cannot execute get request to " + config.groupsUrl(),e );
		}
		
		if (!response.getStatusCode().is2xxSuccessful()) {
			throw new InvalidParameterException("Invalid password");
		}
		
		String raw= response.getBody();
		
		int posStart = raw.indexOf(GROUPS_MARKUP_START);
		int posEnd = raw.indexOf(GROUPS_MARKUP_END);
		

		if (-1 == posStart || -1 == posEnd) {
			throw new TechnicalException("Cannot retrieve groups for " + config.getUser()+". No " +GROUPS_MARKUP_START + " markup found" );
		}
		
		String[] groups = raw.substring(posStart+GROUPS_MARKUP_START.length() +1,posEnd-1).split(",");
		
		return Arrays.asList(groups).stream()
				.map(e -> e.replace("xwiki:XWiki.", "").trim().toUpperCase()).collect(Collectors.toList() );	
		
	}


	/**
	 * Download the full XAR wiki file 
	 * @param destFile
	 * @throws TechnicalException
	 * @throws InvalidParameterException
	 */
	public void exportXwikiContent ( File destFile) throws TechnicalException, InvalidParameterException {

		String plainCreds = config.getUser()+  ":" + config.getPassword();
		byte[] plainCredsBytes = plainCreds.getBytes();
		byte[] base64CredsBytes = Base64.encodeBase64(plainCredsBytes);
		String base64Creds = new String(base64CredsBytes);

		HttpHeaders headers = new HttpHeaders();
		headers.add("Authorization", "Basic " + base64Creds);
	
		final RestTemplate restTemplate = new RestTemplate();
		// Optional Accept header
		RequestCallback requestCallback = request -> {
			
		        IOUtils.write("name=all&description=&licence=&author=XWiki.Admin&version=&history=false&backup=true".getBytes(), request.getBody());
		        request.getHeaders().addAll(headers);
		};
		    
		// Streams the response instead of loading it all in memory
		ResponseExtractor<Void> responseExtractor = response -> {
		    // Here I write the response to a file but do what you like
		    Path path = Paths.get(destFile.getAbsolutePath());
		    Files.copy(response.getBody(), path);
		    return null;
		};
		
		if (destFile.exists()) {
			destFile.delete();
		}
		restTemplate.execute(URI.create( config.getBaseUrl()+ "/xwiki/bin/export/XWiki/XWikiPreferences?editor=globaladmin&section=Export"), HttpMethod.POST, requestCallback, responseExtractor);
	}
	
	
	/**
	 * Get a html content from the wiki, from a simple page (nice for widget rendering)
	 * @param xwikiPath
	 * @param user
	 * @param password
	 * @return
	 * @throws TechnicalException
	 * @throws InvalidParameterException
	 */
	public WikiResult html (String xwikiPath) throws TechnicalException, InvalidParameterException{		
	
		WikiResult cached = contentCache.get(xwikiPath);
		if (null != cached && !StringUtils.isEmpty(cached.getHtml())) {
			return cached;
		}		
		
		WikiResult res = new WikiResult();
		
		String url = config.viewPath() +URLDecoder.decode(xwikiPath, Charset.defaultCharset());		
		
		String plainCreds = config.getUser()+":"+config.getPassword();
		byte[] plainCredsBytes = plainCreds.getBytes();
		byte[] base64CredsBytes = Base64.encodeBase64(plainCredsBytes);
		String base64Creds = new String(base64CredsBytes);

		HttpHeaders headers = new HttpHeaders();
		headers.add("Authorization", "Basic " + base64Creds);
		headers.add("accept", "text/html " );
		
		ResponseEntity<String> response = null;
		try {
			
			HttpEntity<String> request = new HttpEntity<String>(headers);
			response = restTemplate.exchange(url, HttpMethod.GET, request, String.class);
		} catch (Exception e) {
			logger.error("Cannot parse wiki page at {} : {}" , url,e.getMessage());
		}		
		
		if (null != response && response.getStatusCode().is2xxSuccessful()) {
			//throw new InvalidParameterException("Invalid password");
			try {
				String raw= response.getBody();
				int pos=raw.indexOf(MARKER);
				raw = raw.substring(pos+MARKER.length()).trim();
				String body= raw.substring(0,raw.indexOf("\n"));				
				res.setHtml(body);
			}
			catch (Exception e) {
				
				logger.error("Cannot parse wiki page at " + url,e);
//				throw new TechnicalException("Cannot parse wiki page at " + url,e);
			}
		} 
		res.setViewLink(url );
		res.setEditLink(url.replace("/view/", "/edit/"));
		
		// Putting in cache
		contentCache.put(xwikiPath, res);
		return res;
	}
		
	/**
	 * Get a WikiResult content from the wiki, with full metas
	 * @param xwikiPath
	 * @param user
	 * @param password
	 * @return
	 * @throws TechnicalException
	 * @throws InvalidParameterException
	 */
	public WikiResult getPage (String xwikiPath) throws TechnicalException, InvalidParameterException{
		
		
		WikiResult cached = contentCache.get(xwikiPath);
		if (null != cached && !StringUtils.isEmpty(cached.getHtml())) {
			return cached;
		}		
		
		WikiResult res = new WikiResult();
		
		String url = config.viewPath() +URLDecoder.decode(xwikiPath, Charset.defaultCharset());		
		
		String plainCreds = config.getUser()+":"+config.getPassword();
		byte[] plainCredsBytes = plainCreds.getBytes();
		byte[] base64CredsBytes = Base64.encodeBase64(plainCredsBytes);
		String base64Creds = new String(base64CredsBytes);

		HttpHeaders headers = new HttpHeaders();
		headers.add("Authorization", "Basic " + base64Creds);
		headers.add("accept", "text/html " );
		
		ResponseEntity<String> response = null;
		try {
			
			HttpEntity<String> request = new HttpEntity<String>(headers);
			response = restTemplate.exchange(url, HttpMethod.GET, request, String.class);
		} catch (Exception e) {
//			throw new TechnicalException("Cannot execute get request to " + url,e );
			logger.error("Cannot parse wiki page at {} : {}" , url,e.getMessage());
		}		
		
		if (null != response && response.getStatusCode().is2xxSuccessful()) {
			try {
				String raw= response.getBody();
				
				final Document doc = DocumentHelper.getDocument(raw);
		    
				String layout = getDd(doc, 1);
				String width= getDd(doc, 2);
				String metaTitle = getDd(doc, 3);
				String metaDescription = getDd(doc, 4);
				String metaKeyword = getDd(doc, 5);
				String body = getHtmlFromRaw(raw);
				String pageTitle = getDd(doc, 6);
				String author = XpathHelper.xpathEval(doc,"//meta[@name='author']/@content").getTextContent() ;    	
				
				res.setLayout(layout);
				res.setWidth(width);
				res.setMetaTitle(metaTitle);
				res.setMetaDescription(metaDescription);
				res.setMetaKeyword(metaKeyword);
				res.setAuthor(author);
				
				res.setPageName(XpathHelper.xpathEval(doc, "//div[@id='document-title']").getTextContent());
				
				res.setHtml(body);
				res.setPageTitle(pageTitle);
				
			} catch (InvalidParameterException e) {
			} catch (Exception e) {
				
				logger.error("Cannot parse wiki page at " + url,e);
			}
		} 
		res.setViewLink(url );
		res.setEditLink(url.replace("/view/", "/edit/"));
		
		// Putting in cache
		contentCache.put(xwikiPath, res);
		return res;	
	}

	public void invalidate(String doc) {
		logger.warn("Invalidating wiki cache : {}",doc);
		contentCache.remove(doc.replace(".", "/"));
	}

	public void invalidateAll() {
		contentCache.clear();
	}
	
	
	/**
	 * Internal routine to retrieve attributes fiels from the wiki page
	 * @param node
	 * @param num
	 * @return
	 * @throws XPathExpressionException
	 * @throws TechnicalException
	 * @throws ResourceNotFoundException
	 */
	public String getDd(Node node, int num) throws XPathExpressionException, TechnicalException, ResourceNotFoundException {
		String ret = XpathHelper.xpathEval(node,"//div[@id='xwikicontent']//dd["+num+"]").getTextContent();	
		return ret;
	}

	/**
	 * A tricky method to retrieve page content
	 * @return
	 */
	
	private String getHtmlFromRaw(String raw) {
		int pos=raw.indexOf("XWiki.PageClass[0].pageContent");
		int start = raw.indexOf("<dd>", pos) +4;
		int stop = raw.indexOf("</dd>", start);
		String ret= raw.substring(start,stop);
		
		return ret;
	}
	

}