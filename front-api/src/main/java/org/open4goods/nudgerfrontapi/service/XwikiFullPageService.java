package org.open4goods.nudgerfrontapi.service;

import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.Map;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.open4goods.model.constants.CacheConstants;
import org.open4goods.nudgerfrontapi.dto.xwiki.FullPageDto;
import org.open4goods.xwiki.model.FullPage;
import org.open4goods.xwiki.services.XwikiFacadeService;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.xwiki.rest.model.jaxb.Page;

/**
 * Service responsible for retrieving XWiki {@link FullPage} instances and mapping them to {@link FullPageDto}.
 * <p>
 * The service centralises DTO conversions so the rest of the application can work with a
 * simplified representation that hides XWiki specific dependencies.
 * </p>
 */
@Service
public class XwikiFullPageService {

    private final XwikiFacadeService xwikiFacadeService;

    public XwikiFullPageService(XwikiFacadeService xwikiFacadeService) {
        this.xwikiFacadeService = xwikiFacadeService;
    }

    /**
     * Fetches a {@link FullPage} from XWiki and converts it into a {@link FullPageDto} representation.
     *
     * @param pageId      identifier of the page to fetch
     * @param languageTag language tag used to retrieve the localised page
     * @return flattened DTO representation of the XWiki page
     */
    @Cacheable(cacheNames = CacheConstants.ONE_HOUR_LOCAL_CACHE_NAME, keyGenerator = CacheConstants.KEY_GENERATOR)
    public FullPageDto getFullPage(String pageId, String languageTag) {
        FullPage fullPage = xwikiFacadeService.getFullPage(pageId, languageTag);
        return mapToDto(fullPage);
    }

    /**
     * Convert the XWiki {@link FullPage} aggregate to its DTO counterpart.
     *
     * @param fullPage entity returned by the XWiki facade
     * @return flattened DTO compatible with the REST contract
     */
    private FullPageDto mapToDto(FullPage fullPage) {
        Page wikiPage = fullPage.getWikiPage();
        Map<String, String> properties = fullPage.getProperties();
        if (properties == null) {
            properties = Collections.emptyMap();
        }

        // Fetch HTML content separately because the REST descriptor only exposes metadata.
        String htmlContent = xwikiFacadeService.getxWikiHtmlService().getHtmlClassWebPage(fullPage.getWikiPage().getId());
        return new FullPageDto(
        		htmlContent,
                wikiPage != null ? wikiPage.getId() : null,
                wikiPage != null ? wikiPage.getFullName() : null,
                wikiPage != null ? wikiPage.getWiki() : null,
                wikiPage != null ? wikiPage.getSpace() : null,
                wikiPage != null ? wikiPage.getName() : null,
                wikiPage != null ? wikiPage.getTitle() : null,
                wikiPage != null ? wikiPage.getRawTitle() : null,
                wikiPage != null ? wikiPage.getParent() : null,
                wikiPage != null ? wikiPage.getParentId() : null,
                wikiPage != null ? wikiPage.getVersion() : null,
                wikiPage != null ? wikiPage.getAuthor() : null,
                wikiPage != null ? wikiPage.getAuthorName() : null,
                wikiPage != null ? wikiPage.getXwikiRelativeUrl() : null,
                wikiPage != null ? wikiPage.getXwikiAbsoluteUrl() : null,
                wikiPage != null ? wikiPage.getSyntax() : null,
                wikiPage != null ? wikiPage.getLanguage() : null,
                wikiPage != null ? wikiPage.getMajorVersion() : null,
                wikiPage != null ? wikiPage.getMinorVersion() : null,
                wikiPage != null && Boolean.TRUE.equals(wikiPage.isHidden()),
                toIsoString(wikiPage != null ? wikiPage.getCreated() : null),
                wikiPage != null ? wikiPage.getCreator() : null,
                wikiPage != null ? wikiPage.getCreatorName() : null,
                toIsoString(wikiPage != null ? wikiPage.getModified() : null),
                wikiPage != null ? wikiPage.getModifier() : null,
                wikiPage != null ? wikiPage.getModifierName() : null,
                wikiPage != null ? wikiPage.getOriginalMetadataAuthor() : null,
                wikiPage != null ? wikiPage.getOriginalMetadataAuthorName() : null,
                properties.get("layout"),
                properties.get("pageTitle"),
                properties.get("metaTitle"),
                properties.get("width"),
                properties.get("metaDescription"),
                fullPage.getEditLink()
        );
    }

    /**
     * Convert an {@link Calendar} instance into an ISO 8601 string while gracefully handling non Gregorian calendars.
     *
     * @param calendar source calendar
     * @return ISO representation or {@code null} when the source is null
     */
    private String toIsoString(Calendar calendar) {
        if (calendar == null) {
            return null;
        }
        GregorianCalendar gregorianCalendar;
        if (calendar instanceof GregorianCalendar existing) {
            gregorianCalendar = existing;
        } else {
            gregorianCalendar = new GregorianCalendar(calendar.getTimeZone());
            gregorianCalendar.setTimeInMillis(calendar.getTimeInMillis());
        }
        try {
            XMLGregorianCalendar xmlCalendar = DatatypeFactory.newInstance().newXMLGregorianCalendar(gregorianCalendar);
            return xmlCalendar.toXMLFormat();
        } catch (DatatypeConfigurationException e) {
            return gregorianCalendar.toInstant().toString();
        }
    }
}
