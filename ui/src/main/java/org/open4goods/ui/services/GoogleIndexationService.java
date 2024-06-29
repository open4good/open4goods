package org.open4goods.ui.services;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.tika.io.IOUtils;
import org.open4goods.dao.ProductRepository;
import org.open4goods.services.VerticalsConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.batch.BatchRequest;
import com.google.api.client.googleapis.batch.json.JsonBatchCallback;
import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.indexing.v3.Indexing;
import com.google.api.services.indexing.v3.model.PublishUrlNotificationResponse;
import com.google.api.services.indexing.v3.model.UrlNotification;
import com.google.common.collect.Lists;

/**
 * This service allow google pages indexation, through the google indexing api
 * links click
 *
 * TODO : Handle I18n
 * @author Goulven.Furet
 *
 */
public class GoogleIndexationService  {
	
	
    public static List<HttpResponse> addedCalendarsUsingBatch = Lists.newArrayList();
    // TODO : const
    public static final String appName = "nudger";
	private GoogleCredential credentials;
	JsonFactory jsonFactory = new JacksonFactory();
	HttpTransport httpTransport = new NetHttpTransport();

	private ProductRepository productRepository;
	private VerticalsConfigService verticalsConfigService;
	
	private static final Logger LOGGER = LoggerFactory.getLogger(SitemapGenerationService.class);

	
//    private static com.google.api.services.calendar.Calendar client;
	
	public GoogleIndexationService(String googleJsonConfig,  ProductRepository productRepository, VerticalsConfigService verticalsConfigService) {
		
		this.productRepository = productRepository;
		
		String scopes = "https://www.googleapis.com/auth/indexing";
		
		try (InputStream in = IOUtils.toInputStream(googleJsonConfig)){
			this.credentials = GoogleCredential.fromStream(in, httpTransport, jsonFactory).createScoped(Collections.singleton(scopes));
		} catch (IOException e) {
			LOGGER.error("Error while creating google credentials", e);
		}
	}

//	@Scheduled(fixedRate = 1000 * 60 * 60 * 24)
//	// TODO : Timing from conf
//	public void indexNewProducts() {
//		
//		try {
//			Long lastFetch = Long.valueOf(FileUtils.readFileToString(new File(lastIndexationMarkerFile), Charset.forName("UTF-8") ).trim());
//			
//			Object newProducts = productRepository.exportVerticalWithValidDateOrderByEcoscore(verticalId,false)
//					// Filtering on products having genAI content
//					.filter(e -> null != e.getAiDescriptions())
//					// TODO : Not really filtered per language
//					.filter(e -> e.getAiDescriptions().size() > 1)
//					// TODO ; Localisation
//					.map(e -> e.url("fr"))
//					
//					.toList();
//			
//			
//		} catch (NumberFormatException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		
//		
//		
//	}
//	
	
	
    /**
     * Index a list or URL's to google indexation service
     * @param urls
     * @throws IOException 
     */
    public  void requestBatchIndex(List<String> urls) throws IOException {

        		JsonBatchCallback<PublishUrlNotificationResponse> callback = new JsonBatchCallback<PublishUrlNotificationResponse>() {

			    public void onSuccess(PublishUrlNotificationResponse res, HttpHeaders responseHeaders) {
			            LOGGER.info("Google indexation success : " + res.getUrlNotificationMetadata().getUrl());
			    }
			    public void onFailure(GoogleJsonError e, HttpHeaders responseHeaders) {
			    	
		        	LOGGER.error("Error while pushing url to google indexation", e);                        
			    }
			};

			Indexing client = new Indexing(httpTransport, jsonFactory, credentials);
			BatchRequest batch = client.batch();
			batch.setBatchUrl(new GenericUrl("https://indexing.googleapis.com/batch"));

			for (String url : urls) {
			    UrlNotification unf = new UrlNotification();
			    unf.setUrl(url);
			    unf.setType("URL_UPDATED");
			    client.urlNotifications().publish(unf).queue(batch, callback);
			}
			batch.execute();
    }
    
    
    /**
     * Get all urls for a vertical
     * @param verticalId
     * @param baseUrl
     * @return
     */
    private List<String> getVerticalUrls( String verticalId, String baseUrl) {	
		
			try {
				return productRepository.exportVerticalWithValidDateOrderByEcoscore(verticalId,false)
				// Filtering on products having genAI content
						.filter(e -> null != e.getAiDescriptions())
						// TODO : Not really filtered per language
						.filter(e -> e.getAiDescriptions().size() > 1)
						// TODO : i18n
						.map(e -> baseUrl+ e.url("fr"))
						
						.toList();
				
				
			} catch (Exception e) {
				LOGGER.error("Error while exporting vertical", e);
			}
			return new ArrayList<String>();
	}

    
    
    
    
    /**
     * Index a vertical
     * @param verticalId
     * @param baseUrl
     */
	public void indexVertical(String verticalId, String baseUrl) {
		LOGGER.info("Querying Google Indexation of vertical {}", verticalId);
		List<String> urls = getVerticalUrls(verticalId, baseUrl);
		LOGGER.info("Starting Indexation of vertical {} : {} urls", verticalId, urls.size());
		try {
			requestBatchIndex(urls);
		} catch (IOException e) {
			LOGGER.error("Error while pushing verticals urls to google indexation", e);
		}
	}
    

    /**
     * Index a vertical
     * @param verticalId
     * @param baseUrl
     */
	public void indexAllSince(Long epoch) {
		LOGGER.info("Querying Google Indexation of all products since epoch {}", epoch);
		List<String> urls = productRepository.exportAllVerticalizedProductsWithGenAiSinceEpoch(epoch)
		// Filtering on products having genAI content
				.filter(e -> null != e.getAiDescriptions())
				// TODO : Not really filtered per language
				.filter(e -> e.getAiDescriptions().size() > 1)
				// TODO : I18n
				.map(e -> e.url("fr"))
				
				.toList();
		LOGGER.info("Starting Indexation of all products since epoch {} : {} urls", epoch, urls.size());
		
		try {
			requestBatchIndex(urls);
		} catch (IOException e1) {
			LOGGER.error("Error while pushing new urls to google indexation", e1);
		}
	}
    
	
	
//    public  static void main(String[] args) {
//
//    	  
//        
//    	String jsonFile = "/home/Goulven.Furet/git/open4goods-config/configs/ui/google-api.json";
//    	GoogleIndexationService main = new GoogleIndexationService(jsonFile,null);
//    	
//    	List<String> urls = Lists.newArrayList();
//    	urls.add("https://nudger.fr");
//    	
//    	main.requestBatchIndex(urls);
//
//  
//
//}
	
}
