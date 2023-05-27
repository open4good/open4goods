package org.open4goods.crawler;

import java.io.IOException;
import java.time.Duration;

import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.open4goods.config.yml.datasource.CrawlProperties;
import org.open4goods.crawler.config.yml.FetcherProperties;
import org.open4goods.crawler.services.fetching.SeleniumFetchingService;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import edu.uci.ics.crawler4j.crawler.CrawlConfig;
import edu.uci.ics.crawler4j.crawler.exceptions.PageBiggerThanMaxSizeException;
import edu.uci.ics.crawler4j.fetcher.PageFetchResult;
import edu.uci.ics.crawler4j.fetcher.PageFetcher;
import edu.uci.ics.crawler4j.url.WebURL;
import jakarta.annotation.PreDestroy;

/**
 * A crawl4j pagefetcher that relies on Selenium to
 *         extract page content
 */
public class SeleniumPageFetcher extends PageFetcher {
	protected static final Logger logger = LoggerFactory.getLogger(SeleniumPageFetcher.class);

	/**
	 * This field is protected for retro compatibility. Please use the getter
	 * method: getConfig() to read this field;
	 */
	protected final CrawlConfig config;

	private final CrawlProperties crawlProperties;

	private final FetcherProperties fetcherConfig;
	
	@Autowired
	private  SeleniumFetchingService fetchingService;

	public SeleniumPageFetcher(CrawlConfig config, CrawlProperties crawlProperties, FetcherProperties fetcherConfig) {
		super(config);
		this.config = config;
		this.crawlProperties = crawlProperties;
		this.fetcherConfig = fetcherConfig;

	}

	public PageFetchResult fetchPage(WebURL webUrl)
			throws InterruptedException, IOException, PageBiggerThanMaxSizeException {

		// Faking the page fetch result from Selenium
		PageFetchResult fetchResult = new PageFetchResult();
		String url = webUrl.getURL();
		fetchResult.setFetchedUrl(url);

		String content = fetchingService.getHtmlContent(url);
		fetchResult.setStatusCode(200);
		fetchResult.setEntity(new StringEntity(content, ContentType.TEXT_HTML.toString(), crawlProperties.getSeleniumPageEncoding()));

		return fetchResult;
	}

	
	
	private static ChromeDriverService service;

	private static ChromeOptions options;

//	static {
//		// Setting chrome options
//				options = new ChromeOptions();
////				options.addArguments("--headless");
//				options.addArguments("user-agent=" + fetcherConfig.getSeleniumUseragent());
//				options.setExperimentalOption("excludeSwitches", new String[] { "enable-automation" });
//				options.setExperimentalOption("useAutomationExtension", false);
//
//				try {
//					// Setting chromeservice
//					service = new ChromeDriverService.Builder().usingDriverExecutable(new File(fetcherConfig.getChromeDriverPath())).usingAnyFreePort().build();
//					service.start();
//				} catch (IOException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//
//	}

	@PreDestroy
	public void stopService() {
		service.stop();
	}

	public String getHtmlContent(String url) {

		RemoteWebDriver driver = new RemoteWebDriver(service.getUrl(), options);
		
		// TODO : timer from conf
		WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

		driver.navigate().to(url);

		// Click the "ABOUT" button (after it's loaded)
		wait.until(ExpectedConditions.jsReturnsValue("return jQuery.active == 0"));

//			wait.until(ExpectedConditions. visibilityOf(element) .elementToBeClickable(By.id("fpAddBsk")));
//			WebElement elem = driver.findElement(By.tagName("html"));

		String ret = driver.getPageSource();

		// Close the browser window
		driver.quit();

		return ret;
	}

}
