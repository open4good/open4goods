package org.open4goods.xwiki.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.open4goods.xwiki.config.XWikiServiceProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.xwiki.rest.model.jaxb.Attachments;
import org.xwiki.rest.model.jaxb.Link;
import org.xwiki.rest.model.jaxb.Objects;
import org.xwiki.rest.model.jaxb.Page;

class XwikiMappingServiceTest {

    @Mock
    private RestTemplateService restTemplateService;

    private XwikiMappingService mappingService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        XWikiServiceProperties props = new XWikiServiceProperties();
        props.setBaseUrl("http://localhost");
        props.setUsername("user");
        props.setPassword("pass");
        props.setHttpsOnly(false);
        ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mappingService = new XwikiMappingService(restTemplateService, props, mapper);
    }

    @Test
    void mapPageDeserializesCorrectly() {
        String json = "{\"name\":\"WebHome\",\"fullName\":\"Main.WebHome\",\"wiki\":\"xwiki\",\"space\":\"Main\"}";
        ResponseEntity<String> resp = new ResponseEntity<>(json, HttpStatus.OK);
        when(restTemplateService.getRestResponse(anyString())).thenReturn(resp);

        Page page = mappingService.mapPage("dummy");

        assertThat(page.getName()).isEqualTo("WebHome");
        assertThat(page.getWiki()).isEqualTo("xwiki");
    }

    @Test
    void getObjectsDeserializesCorrectly() {
        String json = "{\"objectSummaries\":[{\"className\":\"XWiki.MyClass\",\"number\":0}]}";
        ResponseEntity<String> resp = new ResponseEntity<>(json, HttpStatus.OK);
        when(restTemplateService.getRestResponse(anyString())).thenReturn(resp);

        Objects objects = mappingService.getObjects("dummy");

        assertThat(objects.getObjectSummaries()).hasSize(1);
        assertThat(objects.getObjectSummaries().get(0).getClassName()).isEqualTo("XWiki.MyClass");
    }

    @Test
    void getAttachmentsDeserializesCorrectly() {
        String json = "{\"attachments\":[{\"id\":\"A1\",\"name\":\"file.txt\"}]}";
        ResponseEntity<String> resp = new ResponseEntity<>(json, HttpStatus.OK);
        when(restTemplateService.getRestResponse(anyString())).thenReturn(resp);

        Page page = new Page().withLinks(new Link().withHref("http://example.com/page").withRel("self"));

        Attachments attachments = mappingService.getAttachments(page);

        assertThat(attachments.getAttachments()).hasSize(1);
        assertThat(attachments.getAttachments().get(0).getName()).isEqualTo("file.txt");
    }
}
