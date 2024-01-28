package org.open4goods.services;


import java.io.File;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
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
import org.open4goods.model.dto.WikiAttachment;
import org.open4goods.model.dto.WikiPage;
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
 * This service handles XWiki content bridges :
 * > Authentication, through the registered users/password on the wiki
 * > RBAC, through roles
 * > Content retrieving, through the REST API
 * TODO : This class is a mess
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
	   // Define the formatter for the given date pattern
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssX");

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
		HttpHeaders headers = authenticatedHeaders(user, password);

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

		// Authentication headers
		HttpHeaders headers = authenticatedHeaders(config);

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

		// Authentication headers
		HttpHeaders headers = authenticatedHeaders(config);

		headers.add("accept", "text/html " );

		ResponseEntity<String> response = null;
		try {

			HttpEntity<String> request = new HttpEntity<String>(headers);
			response = restTemplate.exchange(url, HttpMethod.GET, request, String.class);
		} catch (Exception e) {
			logger.error("Cannot render to html wiki page at {} : {}" , url,e.getMessage());
		}

		if (null != response && response.getStatusCode().is2xxSuccessful()) {
			//throw new InvalidParameterException("Invalid password");
			try {
				String raw= response.getBody();
				int pos=raw.indexOf(MARKER);
				raw = raw.substring(pos+MARKER.length()).trim();
				String body= raw.substring(0,raw.indexOf("\n"));

				// Removing simple <p> tag if occurs
				if (body.startsWith("<p>")) {
					body=body.substring(3,body.length()-4);
				}
				res.setHtml(body);
			}
			catch (Exception e) {

				logger.error("Cannot render to html page at " + url,e);
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

		// Authentication headers
		HttpHeaders headers = authenticatedHeaders(config);

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

				final Document doc = DocumentHelper.cleanAndGetDocument(raw);

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
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	/**
	 * List pages in a space
	 * @param space
	 * @return
	 */
	public List<WikiPage> getPages (String space) {
		// https://wiki.nudger.fr/rest/wikis/xwiki/spaces/Blog/pages
		
//		String url = "https://wiki.nudger.fr/rest/wikis/xwiki/spaces/"+space+"/pages";
		
		
		String url = config.restPath() +URLDecoder.decode(space, Charset.defaultCharset())+"/pages";
		logger.info("Getting wiki pages from rest endpoint : {}",url);

		// Authentication headers
		HttpHeaders headers = authenticatedHeaders(config);

		headers.add("accept", "application/xml " );

		ResponseEntity<String> response = null;
		try {

			HttpEntity<String> request = new HttpEntity<String>(headers);
			response = restTemplate.exchange(url, HttpMethod.GET, request, String.class);
		} catch (Exception e) {
			//			throw new TechnicalException("Cannot execute get request to " + url,e );
			logger.error("Cannot parse wiki page at {} : {}" , url,e.getMessage());
		}

		List<WikiPage> pages = new ArrayList<>();
		if (null != response && response.getStatusCode().is2xxSuccessful()) {
			try {
				String raw= response.getBody();

				final Document doc = DocumentHelper.cleanAndGetDocument(raw);

				
				List<String> ids = XpathHelper.xpathMultipleEval(doc,"//id");
				List<String> fullNames = XpathHelper.xpathMultipleEval(doc,"//fullName");
				List<String> wikis = XpathHelper.xpathMultipleEval(doc,"//wiki");
				List<String> spaces = XpathHelper.xpathMultipleEval(doc,"//space");
				List<String> names = XpathHelper.xpathMultipleEval(doc,"//name");
				List<String> titles = XpathHelper.xpathMultipleEval(doc,"//title");
				List<String> rawTitles = XpathHelper.xpathMultipleEval(doc,"//rawTitle");
				List<String> parents = XpathHelper.xpathMultipleEval(doc,"//parent");
				List<String> parentIds = XpathHelper.xpathMultipleEval(doc,"//parentId");
				List<String> versions = XpathHelper.xpathMultipleEval(doc,"//version");
				List<String> authors = XpathHelper.xpathMultipleEval(doc,"//author");
				

				for (int i = 0; i < ids.size(); i++) {
					WikiPage page = new WikiPage();
					page.setAuthor(authors.get(i));
					page.setFullName(fullNames.get(i));
					page.setId(ids.get(i));
					page.setName(names.get(i));
					page.setParent(parents.get(i));
					page.setParentId(parentIds.get(i));
					page.setRawTitle(rawTitles.get(i));
					page.setSpace(spaces.get(i));
					page.setTitle(titles.get(i));
					page.setVersion(versions.get(i));
					page.setWiki(wikis.get(i));	
					
					
					
					Map<String, String> props = fetchClassProperties(page.getSpace(), page.getName(), "Blog.BlogPostClass");
					page.setProps(props);
					pages.add(page);
				}
				
			} catch (Exception e) {
				logger.error("Cannot parse wiki page at " + url,e);
			}
		}
		
		return pages;
		
		
		
	}

	
	/**
	 * Get the URL of an image, given its name and space
	 * @param space
	 * @param name
	 * @param string
	 * @return
	 */
	public String getAttachmentUrl(String space, String name, String attachmentName) {
		
		return config.getBaseUrl()+"/bin/download/"+space+"/"+name+"/"+attachmentName;
	}
	

	/**
	 * Download an attachment, using xwiki authentication
	 * @param url
	 * @return
	 */
	public byte[] downloadAttachment(String url) {
		logger.info("Downloading wiki attachment from {}",url);
		
		// Authentication headers
		HttpHeaders headers = authenticatedHeaders(config);
//		headers.add("accept", "application/xml " );

		ResponseEntity<byte[]> response = null;
		try {

			HttpEntity<String> request = new HttpEntity<String>(headers);
			response = restTemplate.exchange(url, HttpMethod.GET, request, byte[].class);
		} catch (Exception e) {
			//			throw new TechnicalException("Cannot execute get request to " + url,e );
			logger.error("Cannot parse wiki page at {} : {}" , url,e.getMessage());
		}
		
		return response.getBody();
		
	}
	/**
	 * Retrieve a page, with properties and attachments
	 * @param space
	 * @param pageName
	 * @return
	 */
	public WikiPage getPage(String space, String pageName) {
		// https://wiki.nudger.fr/rest/wikis/xwiki/spaces/Blog/pages
		
		WikiPage page = new WikiPage();
//		String url = "https://wiki.nudger.fr/rest/wikis/xwiki/spaces/"+space+"/pages";
		String url = config.restPath() +URLDecoder.decode(space, Charset.defaultCharset())+"/pages/"+URLDecoder.decode(pageName, Charset.defaultCharset());
		logger.info("Getting wiki page {}:{} from rest endpoint : {}",space,pageName,url);
	
		// Authentication headers
		HttpHeaders headers = authenticatedHeaders(config);
		headers.add("accept", "application/xml " );

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

				final Document doc = DocumentHelper.cleanAndGetDocument(raw);

				String id = XpathHelper.xpathEval(doc,"//id").getTextContent();
				String fullName = XpathHelper.xpathEval(doc,"//fullName").getTextContent();
				String wiki = XpathHelper.xpathEval(doc,"//wiki").getTextContent();
				String name = XpathHelper.xpathEval(doc,"//page/name").getTextContent();
				String title = XpathHelper.xpathEval(doc,"//title").getTextContent();
				String rawTitle = XpathHelper.xpathEval(doc,"//rawTitle").getTextContent();
				String parent = XpathHelper.xpathEval(doc,"//parent").getTextContent();
				String parentId = XpathHelper.xpathEval(doc,"//parentId").getTextContent();
				String version = XpathHelper.xpathEval(doc,"//version").getTextContent();
				String author = XpathHelper.xpathEval(doc,"//author").getTextContent();
				String language = XpathHelper.xpathEval(doc,"//language").getTextContent();
				String majorVersion = XpathHelper.xpathEval(doc,"//majorVersion").getTextContent();
				String minorVersion = XpathHelper.xpathEval(doc,"//minorVersion").getTextContent();
				String hidden = XpathHelper.xpathEval(doc,"//hidden").getTextContent();
				String created = XpathHelper.xpathEval(doc,"//created").getTextContent();
				String creator = XpathHelper.xpathEval(doc,"//creator").getTextContent();
				String modified = XpathHelper.xpathEval(doc,"//modified").getTextContent();
				String modifier = XpathHelper.xpathEval(doc,"//modifier").getTextContent();
				String content = XpathHelper.xpathEval(doc,"//content").getTextContent();
						String originalMetadataAuthor = XpathHelper.xpathEval(doc,"//originalMetadataAuthor").getTextContent();;
				
				page.setAuthor(author);
				page.setFullName(fullName);
				page.setId(id);
				page.setName(name);
				page.setParent(parent);
				page.setParentId(parentId);
				page.setRawTitle(rawTitle);
				page.setSpace(space);
				page.setTitle(title);
				page.setVersion(version);
				page.setWiki(wiki);
				page.setContent(content);
				page.setLanguage(language);
				page.setMajorVersion(majorVersion);
				page.setMinorVersion(minorVersion);
					
				page.setHidden(hidden);
				page.setCreated(parseWikiDate(created));
				page.setCreator(creator);
				page.setModified(parseWikiDate(modified));
				page.setModifier(modifier);
				page.setOriginalMetadataAuthor(originalMetadataAuthor);
				
	
			} catch (Exception e) {
				logger.error("Cannot parse wiki page at " + url,e);
			}
		}
		
		page.setAttachments(fetchAttachments(space, pageName));
		page.setProps(fetchClassProperties(space, pageName, "Blog.BlogPostClass"));
		
		return page;
		
	}

	/**
	 * Fetch the properties of a class, as a map 
	 * @param space
	 * @param pageName
	 * @param wClass
	 * @return
	 */
	public Map<String,String> fetchClassProperties(String space, String pageName, String wClass) {
//		https://wiki.nudger.fr/rest/wikis/xwiki/spaces/Blog/pages/BlogIntroduction/objects/Blog.BlogPostClass/0
//			
//			
//			TODO : Extend
//			
		String url = config.restPath() +URLDecoder.decode(space, Charset.defaultCharset())+"/pages/"+URLDecoder.decode(pageName, Charset.defaultCharset() )+"/objects/"+wClass+"/0";
		logger.info("Getting blog properties {}:{} from rest endpoint : {}",space,pageName,url);
		
		
		Map<String,String> ret = new HashMap<>();
		
		logger.info("Completing wiki pages from rest endpoint : {}",url);

		// Authentication headers
		HttpHeaders headers = authenticatedHeaders(config);
		headers.add("accept", "application/xml " );

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
				
				List<String> keys = XpathHelper.xpathMultipleEval(doc,"//property/@name");
				List<String> values = XpathHelper.xpathMultipleEval(doc,"//property/value");
				
				for (int i = 0; i < keys.size(); i++) {
					ret.put(keys.get(i), values.get(i));
				}
			
				
				
				
			} catch (Exception e) {
				logger.error("Cannot parse wiki page at " + url,e);
			}
		}
		return ret;
	}
	
	
	/**
	 * Fetch the attachments of a page
	 * @param space
	 * @param pageName
	 * @param wClass
	 * @return
	 */
	public List<WikiAttachment> fetchAttachments(String space, String pageName) {
//		https://wiki.nudger.fr/rest/wikis/xwiki/spaces/Blog/pages/BlogIntroduction/objects/Blog.BlogPostClass/0
//			
//			
//			TODO : Extend
//			
		String url = config.restPath() +URLDecoder.decode(space, Charset.defaultCharset())+"/pages/"+URLDecoder.decode(pageName, Charset.defaultCharset() )+"/attachments/";
		logger.info("Getting attachments {}:{} from rest endpoint : {}",space,pageName,url);
		
		
		List<WikiAttachment> ret = new ArrayList<>();

		// Authentication headers
		HttpHeaders headers = authenticatedHeaders(config);
		headers.add("accept", "application/xml " );

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

				final Document doc = DocumentHelper.cleanAndGetDocument(raw);
				
				List<String> links = XpathHelper.xpathMultipleEval(doc,"//attachment/link[@rel='http://www.xwiki.org/rel/attachmentData']/@href");
				List<String> id = XpathHelper.xpathMultipleEval(doc,"//attachment/id");
				List<String> name = XpathHelper.xpathMultipleEval(doc,"//attachment/name");
				List<String> longSize = XpathHelper.xpathMultipleEval(doc,"//attachment/longSize");
				List<String> version = XpathHelper.xpathMultipleEval(doc,"//attachment/version");
				List<String> pageId = XpathHelper.xpathMultipleEval(doc,"//attachment/pageId");
				List<String> pageVersion = XpathHelper.xpathMultipleEval(doc,"//attachment/pageVersion");
				List<String> mimeType = XpathHelper.xpathMultipleEval(doc,"//attachment/mimeType");
				List<String> author = XpathHelper.xpathMultipleEval(doc,"//attachment/author");
				List<String> date = XpathHelper.xpathMultipleEval(doc,"//attachment/date");
				
				for (int i = 0; i < id.size(); i++) {
					WikiAttachment attachment = new WikiAttachment();
					
					attachment.setUrl(links.get(i));
					attachment.setId(id.get(i));
					attachment.setName(name.get(i));
					attachment.setSize(longSize.get(i));
					attachment.setVersion(version.get(i));
					attachment.setPageId(pageId.get(i));
					attachment.setPageVersion(pageVersion.get(i));
					attachment.setMimeType(mimeType.get(i));
					attachment.setAuthor(author.get(i));
					attachment.setDate(date.get(i));
				}
			
			} catch (Exception e) {
				logger.error("Cannot parse wiki page at " + url,e);
			}
		}
		return ret;
	}
	
	
	
	/**
	 * Get a page, with properties and attachments
	 * 
	 * @param space
	 * @param pageName
	 * @return
	 */
	public long parseWikiDate (String date) {
        // Parse the date string into a LocalDateTime object
        LocalDateTime dateTime = LocalDateTime.parse(date, formatter);
        // Convert LocalDateTime to epoch seconds (Unix timestamp)
       return dateTime.toEpochSecond(ZoneOffset.UTC);
	}
	
	
//	
//    public String renderXWiki20SyntaxAsXHTML(String contentXwiki21) throws ConversionException, ComponentLookupException
//    {
//        // Initialize Rendering components and allow getting instances
//        EmbeddableComponentManager cm = new EmbeddableComponentManager();
//        cm.initialize(this.getClass().getClassLoader());
//
//        // Use the Converter component to convert between one syntax to another.
//        Converter converter = cm.getInstance(Converter.class);
//
//        // Convert input in XWiki Syntax 2.1 into XHTML. The result is stored in the printer.
//        WikiPrinter printer = new DefaultWikiPrinter();
//        converter.convert(new StringReader(contentXwiki21), Syntax.XWIKI_2_1, Syntax.XHTML_1_0, printer);
//
//        return printer.toString();
//       
//    }
//
//	
	
	
	
	
	
	
	
	
	
	/**
	 * Retrieve http headers that will allow to authenticate against the wiki
	 * @param user
	 * @param password
	 * @return
	 */
	private HttpHeaders authenticatedHeaders(String user, String password) {
		String plainCreds = user +":"+password;
		byte[] plainCredsBytes = plainCreds.getBytes();
		byte[] base64CredsBytes = Base64.encodeBase64(plainCredsBytes);
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
	private HttpHeaders authenticatedHeaders(XwikiConfiguration config) {
		return authenticatedHeaders(config.getUser(), config.getPassword());
	}


	
	
}