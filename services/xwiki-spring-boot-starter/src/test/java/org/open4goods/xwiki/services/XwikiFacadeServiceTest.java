package org.open4goods.xwiki.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.open4goods.xwiki.config.XWikiServiceProperties;
import org.open4goods.xwiki.model.FullPage;
import org.open4goods.xwiki.services.XWikiReadService.PageTranslation;
import org.xwiki.rest.model.jaxb.Page;

@ExtendWith(MockitoExtension.class)
class XwikiFacadeServiceTest {

    @Mock
    private XwikiMappingService mappingService;

    @Mock
    private XWikiObjectService xWikiObjectService;

    @Mock
    private XWikiHtmlService xWikiHtmlService;

    @Mock
    private XWikiReadService xWikiReadService;

    @Mock
    private XWikiServiceProperties properties;

    private XwikiFacadeService facade;

    @BeforeEach
    void setUp() {
        given(properties.getBaseUrl()).willReturn("https://wiki");
        given(properties.getApiEntrypoint()).willReturn("/rest");
        given(properties.getApiWiki()).willReturn("xwiki");
        facade = new XwikiFacadeService(mappingService, xWikiObjectService, xWikiHtmlService, xWikiReadService, properties);
    }

    @Test
    void getFullPageReturnsTranslatedContentWhenAvailable() {
        Page defaultPage = new Page();
        defaultPage.setLanguage("default");
        Page translated = new Page();
        translated.setLanguage("fr");

        given(xWikiReadService.getPage("Main")).willReturn(defaultPage);
        given(xWikiReadService.getPageTranslation(eq(defaultPage), eq("Main"), eq("fr")))
                .willReturn(new PageTranslation(translated, "fr"));
        given(xWikiHtmlService.html("Main", "fr")).willReturn("<p>fr</p>");
        given(xWikiObjectService.getProperties(translated)).willReturn(Map.of("title", "Titre"));

        FullPage page = facade.getFullPage("Main", "fr", Locale.FRENCH);

        assertThat(page.getHtmlContent()).isEqualTo("<p>fr</p>");
        assertThat(page.getResolvedLanguage()).isEqualTo("fr");
        assertThat(page.getProperties()).containsEntry("title", "Titre");
    }

    @Test
    void getFullPageFallsBackToDefaultWhenTranslationMissing() {
        Page defaultPage = new Page();
        defaultPage.setLanguage("default");

        given(xWikiReadService.getPage("Main")).willReturn(defaultPage);
        given(xWikiReadService.getPageTranslation(eq(defaultPage), eq("Main"), eq("fr")))
                .willReturn(null);
        given(xWikiHtmlService.html("Main", "fr")).willReturn(null);
        given(xWikiHtmlService.html("Main")).willReturn("<p>default</p>");
        given(xWikiObjectService.getProperties(defaultPage)).willReturn(Map.of());

        FullPage page = facade.getFullPage("Main", "fr", Locale.FRENCH);

        assertThat(page.getHtmlContent()).isEqualTo("<p>default</p>");
        assertThat(page.getResolvedLanguage()).isEqualTo("default");
        verify(xWikiHtmlService).html("Main", "fr");
        verify(xWikiHtmlService).html("Main");
    }
}
