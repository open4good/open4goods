package org.open4goods.xwiki;


import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.open4goods.xwiki.authentication.XwikiAuthenticationProvider;
import org.open4goods.xwiki.config.XWikiServiceProperties;
import org.open4goods.xwiki.services.RestTemplateService;
import org.open4goods.xwiki.services.XWikiAuthenticationService;
import org.open4goods.xwiki.services.XWikiHtmlService;
import org.open4goods.xwiki.services.XWikiObjectService;
import org.open4goods.xwiki.services.XWikiReadService;
import org.open4goods.xwiki.services.XwikiFacadeService;
import org.open4goods.xwiki.services.XwikiMappingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Ticker;

/**
 * This Configuration class handles :
 * 		xwiki services Beans
 * 		restTemplates Beans for specific call
 * 
 * @author Thierry.Ledan
 */
@AutoConfiguration
@EnableConfigurationProperties(XWikiServiceProperties.class)
public class XWikiServiceConfiguration {
	
	private static final Logger logger = LoggerFactory.getLogger(XWikiServiceConfiguration.class);

	// Cache name used for caching the result of the login method
	public static final String ONE_HOUR_LOCAL_CACHE_NAME = "ONE_HOUR_LOCAL_CACHE";

	private XWikiServiceProperties xWikiProperties;

	@Autowired 
	RestTemplateBuilder localRestTemplateBuilder;
	
	
	public XWikiServiceConfiguration(XWikiServiceProperties xWikiProps) {
		this.xWikiProperties = xWikiProps;
	}

	

	/**
	 * restTemplate dedicated to restful api request
	 * 
	 * @param builder
	 * @return
	 */
	@Bean(name = "xwikiFacadeService")
	
	XwikiFacadeService xwikiFacadeService(
										  @Autowired XwikiMappingService mappingService,
										  @Autowired XWikiReadService xWikiReadService,
										  @Autowired XWikiHtmlService xWikiHtmlService,
										  @Autowired XWikiObjectService xWikiObjectService) {
		logger.info("Creating xwikiFacadeservice");
		return new XwikiFacadeService(mappingService, xWikiObjectService, xWikiHtmlService, xWikiReadService, xWikiObjectService, xWikiHtmlService, xWikiProperties);
	}
	
	/**
	 * restTemplate dedicated to restful api request
	 * 
	 * @param builder
	 * @return
	 */
	@Bean( name = "restTemplate" )
	RestTemplate restTemplate(RestTemplateBuilder builder) {
	
		
		//TOODO : WTF : If resttemplate is build from builder, authentication does not works ! (global or by header in the services)
//		RestTemplate restTemplate =  builder
//				.basicAuthentication(xWikiProperties.getUsername(), xWikiProperties.getPassword())
//				.build();
		//restTemplate.setUriTemplateHandler(new DefaultUriBuilderFactory(xWikiProperties.getApiEntrypoint()));
//		logger.info("RestTemplate created with basic authentication to request XWIKI RESTFUL API SERVER");
		return new RestTemplate();
	}

	/**
	 * restTemplate dedicated to web request 
	 * @param builder
	 * @return
	 */
	@Bean( name = "webTemplate" )
	RestTemplate webTemplate(RestTemplateBuilder builder) {
		
		RestTemplate webTemplate =  
				builder
//					.basicAuthentication(xWikiProperties.getUsername(), xWikiProperties.getPassword())
					.defaultHeader("accept", "text/html ").
				build();
		
		logger.info("WebTemplate created with basic authentication and headers to request XWIKI WEB SERVER");
		return webTemplate;
	}
	
	
	/**
	 * restTemplate services
	 * manage specific restTemplate instances with preset headers for following purposes:
	 *  -> requesting the xwiki rest api
	 *  -> requesting the xwiki server for html response
	 *  -> 
	 * 
	 * @param restTemplate
	 * @param webTemplate
	 * @return
	 */
	@Bean( "restTemplateService" )
	RestTemplateService getRestTemplateService( 
			@Qualifier("restTemplate") RestTemplate restTemplate, 
			@Qualifier("webTemplate") RestTemplate webTemplate
			) {
		
		RestTemplateService restTemplateService = null;
		try {
			restTemplateService = new RestTemplateService(restTemplate, webTemplate, xWikiProperties);
		} catch(Exception e) {
			  logger.error("Unable to create RestTemplateService as bean. error message {}", e.getMessage());
		}
		return restTemplateService;
	}
	
	/**
	 * xwiki objects mapping 
	 * 
	 * @param mappingTemplate
	 * @return
	 */
	@Bean( "mappingService" )
	XwikiMappingService getMappingervice( 
			@Qualifier("restTemplateService") RestTemplateService restTemplateService
			) {
		
		XwikiMappingService mappingService = null;
		try {
			mappingService = new XwikiMappingService(restTemplateService, xWikiProperties);
		} catch(Exception e) {
			  logger.error("Unable to create MappingService as bean. error message {}", e.getMessage());
		}
		return mappingService;
	}	
	
	/**
	 * rest READ Services 
	 * 
	 * @param mappingTemplate
	 * @return
	 */
	@Bean( name = "xwikiReadService" )
	XWikiReadService getXwikiReadService( @Qualifier("mappingService") XwikiMappingService mappingService, RestTemplateService restTemplateService ) {
		
		XWikiReadService XWikiReadService = null;
		try {
			XWikiReadService = new XWikiReadService(mappingService, xWikiProperties, restTemplateService);
		} catch(Exception e) {
			  logger.error("Unable to create XWikiReadService bean. error message {}", e.getMessage());
		}
		return XWikiReadService;
	}
	

	/**
	 * HTML Services 
	 * 
	 * @param mappingTemplate
	 * @return
	 */
	@Bean( name = "xwikiHtmlService" )
	XWikiHtmlService getXwikiHtmlService( 
			// TODO:check what is really needed !!
			@Qualifier("mappingService") XwikiMappingService mappingService,
			@Qualifier("restTemplateService") RestTemplateService restTemplateService
			) {
		
		XWikiHtmlService xwikiHtmlService = null;
		try {
			xwikiHtmlService = new XWikiHtmlService(mappingService, restTemplateService, xWikiProperties);
		} catch(Exception e) {
			  logger.error("Unable to create XWikiHtmlService as bean. error message {}", e.getMessage());
		}
		return xwikiHtmlService;
	}
	
	/**
	 * HTML Services 
	 * 
	 * @param mappingTemplate
	 * @return
	 */
	@Bean( name = "xwikiObjectService" )
	XWikiObjectService getXwikiObjectService( @Qualifier("mappingService") XwikiMappingService mappingService ) {
		
		XWikiObjectService xwikiObjectService = null;
		try {
			xwikiObjectService = new XWikiObjectService(mappingService, xWikiProperties);
		} catch(Exception e) {
			  logger.error("Unable to create XWikiService as bean. error message {}", e.getMessage());
		}
		return xwikiObjectService;
	}
	

	/**
	 * Services related to authentication
	 * 
	 * @param mappingTemplate
	 * @param restTemplateService
	 * @return
	 */
	@Bean( name = "xwikiAuthenticationService" )
	XWikiAuthenticationService getXwikiAuthenticationService( 
			@Qualifier("mappingService") XwikiMappingService mappingService,
			@Qualifier("restTemplateService") RestTemplateService restTemplateService
			) {
		
		XWikiAuthenticationService xWikiAuthenticationService = null;
		try {
			xWikiAuthenticationService = new XWikiAuthenticationService(mappingService, restTemplateService, xWikiProperties, localRestTemplateBuilder);
		} catch(Exception e) {
			  logger.error("Unable to create XWikiAuthenticationService as bean. error message {}", e.getMessage());
		}
		return xWikiAuthenticationService;
	}
	
	
	/**
	 * Basic authentication provider
	 * 
	 * @param xwikiService
	 * @return
	 */
	@Bean( name = "xwikiAuthenticationProvider" )
	XwikiAuthenticationProvider getAuthenticationProvider(@Autowired XWikiAuthenticationService xwikiAuthService) {
		return new XwikiAuthenticationProvider(xwikiAuthService);
	}
	
	
//	
//	@Bean
//	CacheManager cacheManager(@Autowired final Ticker ticker) {
//		final CaffeineCache hCache = buildCache(ONE_HOUR_LOCAL_CACHE_NAME, ticker, 60);
//		final SimpleCacheManager manager = new SimpleCacheManager();
//		manager.setCaches(Arrays.asList( hCache));
//		return manager;
//	}
//
//	private CaffeineCache buildCache(final String name, final Ticker ticker, final int minutesToExpire) {
//		return new CaffeineCache(name,
//				Caffeine.newBuilder().expireAfterWrite(minutesToExpire, TimeUnit.MINUTES).ticker(ticker).build());
//	}
//
//	@Bean
//	Ticker ticker() {
//		return Ticker.systemTicker();
//	}
	
//	@Bean
// TODO  : Test with RestClient or remove it
//	RestClient restClient(RestClient.Builder builder) {
//		String auth = xWikiProperties.getUsername() + ":" + xWikiProperties.getPassword();
//		
//		RestClient restClient = builder
//				.baseUrl(xWikiProperties.getBaseUrl())
//				.defaultHeader("Authorization", "Basic "+Base64.getEncoder().encode(auth.getBytes(Charset.forName("US-ASCII"))))
//				.build();
//		logger.info("RestClient Bean created with basic authentication to " + xWikiProperties.getBaseUrl());
//		return restClient;
//		
//	}
}
