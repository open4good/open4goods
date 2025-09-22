package org.open4goods.xwiki.services;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.open4goods.xwiki.config.UrlManagementHelper;
import org.open4goods.xwiki.config.XWikiConstantsResourcesPath;
import org.open4goods.xwiki.config.XWikiServiceProperties;
import org.open4goods.xwiki.model.FullPage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.rest.model.jaxb.Attachment;
import org.xwiki.rest.model.jaxb.Page;
import org.xwiki.rest.model.jaxb.Pages;
import java.nio.charset.StandardCharsets;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.web.util.UriUtils;

/**
 * An Xwiki facade service, which encapsulates xwiki unitary services to deliver
 * high level  wiki content to spring boot web translation
 */
public class XwikiFacadeService {

	private static Logger LOGGER = LoggerFactory.getLogger(XwikiFacadeService.class);

	// The unitary services
	private final XwikiMappingService mappingService;
	private final XWikiReadService xWikiReadService;
	private final XWikiHtmlService xWikiHtmlService;
	private final XWikiObjectService xWikiObjectService;

	private XWikiServiceProperties properties;

	private UrlManagementHelper urlHelper;

	private XWikiConstantsResourcesPath pathHelper;


        public XwikiFacadeService(XwikiMappingService mappingService,
                        XWikiObjectService objectService,
                        XWikiHtmlService htmlService,
                        XWikiReadService readService,
                        XWikiServiceProperties properties) {
                this.mappingService = mappingService;
                this.xWikiObjectService = objectService;
                this.xWikiHtmlService = htmlService;
                this.xWikiReadService = readService;
                this.properties = properties;
                this.urlHelper = new UrlManagementHelper(properties);
                this.pathHelper = new XWikiConstantsResourcesPath(properties.getBaseUrl(), properties.getApiEntrypoint(), properties.getApiWiki());
        }

        public FullPage getFullPage(String restPath) {
                return getFullPage(restPath, null, null);
        }

        public FullPage getFullPage(String restPath, String requestedLanguage, Locale locale) {
		String htmlPath = normaliseHtmlPath(restPath);
		ResolvedPage resolved = resolveLocalizedContent(restPath, htmlPath, requestedLanguage, locale);

		Map<String, String> properties = xWikiObjectService.getProperties(resolved.page());

		FullPage ret = new FullPage();
		ret.setHtmlContent(resolved.htmlContent());
		ret.setWikiPage(resolved.page());
		ret.setProperties(properties);
		ret.setResolvedLanguage(resolved.resolvedLanguage());
		return ret;
	}

        public LocalizedHtml getLocalizedBloc(String blocId, String requestedLanguage, Locale locale) {
                ResolvedPage resolved = resolveLocalizedContent(blocId, blocId, requestedLanguage, locale);
                return new LocalizedHtml(resolved.htmlContent(), resolved.resolvedLanguage());
        }

        public FullPage getFullPage(String space, String name) {
                return getFullPage(space+":"+name);
        }

        private ResolvedPage resolveLocalizedContent(String restPath, String htmlPath, String requestedLanguage, Locale locale) {
                Page defaultPage = xWikiReadService.getPage(restPath);
                List<String> candidateLanguages = determineCandidateLanguages(requestedLanguage, locale);

                for (String candidate : candidateLanguages) {
                        XWikiReadService.PageTranslation translation = xWikiReadService.getPageTranslation(defaultPage, restPath, candidate);
                        if (translation != null && translation.page() != null) {
                                String html = xWikiHtmlService.html(htmlPath, translation.language());
                                if (StringUtils.isNotBlank(html)) {
                                        String resolvedLanguage = determineResolvedLanguage(translation.page(), translation.language());
                                        return new ResolvedPage(translation.page(), html, resolvedLanguage);
                                }
                        }
                }

                String defaultHtml = xWikiHtmlService.html(htmlPath);
                String resolvedLanguage = determineResolvedLanguage(defaultPage, null);
                return new ResolvedPage(defaultPage, defaultHtml, resolvedLanguage);
        }

        private List<String> determineCandidateLanguages(String requestedLanguage, Locale locale) {
                Set<String> candidates = new LinkedHashSet<>();
                String normalizedRequested = normalizeLanguageToken(requestedLanguage);
                if (normalizedRequested != null) {
                        candidates.add(normalizedRequested);
                        String normalizedLocale = normalizeLanguageToken(locale != null ? locale.toLanguageTag() : null);
                        if (normalizedLocale != null) {
                                candidates.add(normalizedLocale);
                        }
                }
                return new ArrayList<>(candidates);
        }

        private String normalizeLanguageToken(String value) {
                if (!StringUtils.hasText(value)) {
                        return null;
                }
                String token = value.trim().replace('_', '-').toLowerCase(Locale.ROOT);
                int separator = token.indexOf('-');
                if (separator > 0) {
                        token = token.substring(0, separator);
                }
                if (token.length() < 2) {
                        return null;
                }
                for (char character : token.toCharArray()) {
                        if (character < 'a' || character > 'z') {
                                return null;
                        }
                }
                return token;
        }

        private String determineResolvedLanguage(Page page, String fallbackLanguage) {
                String pageLanguage = page != null ? StringUtils.trimToNull(page.getLanguage()) : null;
                if (StringUtils.isNotBlank(pageLanguage)) {
                        return pageLanguage;
                }
                if (StringUtils.isNotBlank(fallbackLanguage)) {
                        return fallbackLanguage;
                }
                return "default";
        }

        private String normaliseHtmlPath(String restPath) {
                if (restPath == null) {
                        return "";
                }
                return restPath.replace('.', '/').replace(':', '/');
        }

        private record ResolvedPage(Page page, String htmlContent, String resolvedLanguage) { }

        public record LocalizedHtml(String htmlContent, String resolvedLanguage) { }



        /**
         *
         * @param url
	 * TODO : Should provide a streamed version
	 * @return
	 */
	public byte[] downloadAttachment( String space, String page, String attachmentName) {
		String url = pathHelper.getDownloadAttachlmentUrl(space, page, attachmentName);
		return mappingService.downloadAttachment(url);
	}



    public byte[] downloadAttachment(String attachmentPath) {
            if (attachmentPath.contains("..")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid attachment path");
            }
            String encodedPath = UriUtils.encodePath(attachmentPath, StandardCharsets.UTF_8);
            String url = pathHelper.getDownloadpath() + encodedPath;
            return mappingService.downloadAttachment(url);
    }


	public String detectMimeType (String filename) {
        // TODO : ugly, should fetch the meta (mime type is availlable in xwiki service), but does not work for the blog image, special class and not appears in attachments list
		if (filename.endsWith(".pdf")) {
			return("application/pdf");
		} else if (filename.endsWith(".jpg") || filename.endsWith(".jpeg")) {
			return("image/jpeg");
		} else if (filename.endsWith(".png")) {
			return("image/png");
		} else if (filename.endsWith(".gif")) {
			return("image/gif");
		}else {
			LOGGER.error("Unknown mime type mapping in XwikiFacadeService for : {}",filename);
			return "";
		}
	}







	// TODO : Remove, or be more exaustiv
	public Pages getPages(String path) {
		return xWikiReadService.getPages(path);
	}

	public XwikiMappingService getMappingService() {
		return mappingService;
	}

	public XWikiReadService getxWikiReadService() {
		return xWikiReadService;
	}

	public XWikiHtmlService getxWikiHtmlService() {
		return xWikiHtmlService;
	}

	public XWikiObjectService getxWikiObjectService() {
		return xWikiObjectService;
	}

	public UrlManagementHelper getUrlHelper() {
		return urlHelper;
	}

	public void setUrlHelper(UrlManagementHelper urlHelper) {
		this.urlHelper = urlHelper;
	}

	public XWikiServiceProperties getProperties() {
		return properties;
	}

	public void setProperties(XWikiServiceProperties properties) {
		this.properties = properties;
	}

	public XWikiConstantsResourcesPath getPathHelper() {
		return pathHelper;
	}

	public void setPathHelper(XWikiConstantsResourcesPath pathHelper) {
		this.pathHelper = pathHelper;
	}






}
