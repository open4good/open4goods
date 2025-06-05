package org.open4goods.xwiki;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.open4goods.xwiki.services.XWikiHtmlService;
import org.open4goods.xwiki.services.XWikiReadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = XWikiServiceConfiguration.class)
@AutoConfigureWebClient
@ActiveProfiles("test")
public class XWikiServicesTest {

	@Autowired XWikiReadService xwikiReadService; 

	@Autowired XWikiHtmlService xwikiHtmlService; 
	
	
	
	
	
//	@Test
//	void testHtmlRendering() {
//		
//		String content = xwikiHtmlService.renderXWiki20SyntaxAsXHTML("= Bienvenue sur votre Wiki =\n");
//		
//		
//		System.out.println(content);
//	}
	
	
	
	
	@Test
	void testEmpty() {
		
		
		//xwikiReadService.downloadAttachment("sss");
		
		
		assertEquals(true, true);
		
	}
}
