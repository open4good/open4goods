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
import com.google.api.client.json.gson.GsonFactory;
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
	
	
    private static final String GOOGLE_API_SCOPE = "https://www.googleapis.com/auth/indexing";
	public static List<HttpResponse> addedCalendarsUsingBatch = Lists.newArrayList();
    // TODO : const
    public static final String appName = "nudger";
	private GoogleCredential credentials;
	JsonFactory jsonFactory = new GsonFactory();
	HttpTransport httpTransport = new NetHttpTransport();

	private ProductRepository productRepository;
	private VerticalsConfigService verticalsConfigService;
	private String markerFileName;
	
	private static final Logger LOGGER = LoggerFactory.getLogger(SitemapGenerationService.class);

	
//    private static com.google.api.services.calendar.Calendar client;
	
	public GoogleIndexationService(String googleJsonConfig, String markerFileName, ProductRepository productRepository, VerticalsConfigService verticalsConfigService) {
		
		this.productRepository = productRepository;
		this.markerFileName = markerFileName;
		
		
		String scopes = GOOGLE_API_SCOPE;
		
		if (null == googleJsonConfig) {
			LOGGER.error("No google json config provided");
		} else {
			try (InputStream in = IOUtils.toInputStream(googleJsonConfig)){
				this.credentials = GoogleCredential.fromStream(in, httpTransport, jsonFactory).createScoped(Collections.singleton(scopes));
			} catch (IOException e) {
				LOGGER.error("Error while creating google credentials", e);
			}
		}
	}

	@Scheduled(fixedRate = 1000 * 60 * 60 * 12)
	// TODO : Timing from conf
	public void indexNewProducts() {		
			indexAllSince(readLastTimeStamp());
	}
	
	

	/**
	 * Read the last indexation timestamp
	 * 
	 * @return
	 */
	private long readLastTimeStamp() {
		Long ret;
		try {
			ret = Long.valueOf(FileUtils.readFileToString(new File(markerFileName), Charset.forName("UTF-8")).trim());
		} catch (Exception e) {
			LOGGER.error("Error while reading last indexation timestamp, will default it ({})", e.getMessage());
			ret = System.currentTimeMillis();
			
			// Writing the file
			writeFileTimestamp();
		}
		return ret;
	}	
	
	/** 
	 * update the indexation timestamp
	 */
    private void writeFileTimestamp() {
		Long now = System.currentTimeMillis();
		try {
			FileUtils.writeStringToFile(new File(markerFileName), now.toString(), Charset.forName("UTF-8"));
		} catch (IOException e) {
			LOGGER.error("Error while writing last indexation timestamp", e);
		}
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
     * Index all new products having texts since a given epoch
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
		LOGGER.warn("Starting Indexation of all products since epoch {} : {} urls", epoch, urls.size());
		
		try {
			requestBatchIndex(urls);
		} catch (IOException e1) {
			LOGGER.error("Error while pushing new urls to google indexation", e1);
		}
	}
	
	/**
	 * Index a single page
	 * @param redircectUrl
	 */
	public void indexPage(String url) {
		List<String> urls = Lists.newArrayList();
        urls.add(url);
        try {
            requestBatchIndex(urls);
        } catch (IOException e) {
            LOGGER.error("Error while pushing url {} to google indexation", url, e);
        }
    }
	
	
	
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
			    	
		        	try {
						LOGGER.error("Error while pushing url to google indexation : {}", e.toPrettyString());
					} catch (IOException e1) {
						LOGGER.error("Error while pushing url to google indexation", e);
					}                        
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
}
	
