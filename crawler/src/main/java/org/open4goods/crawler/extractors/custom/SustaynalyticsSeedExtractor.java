package org.open4goods.crawler.extractors.custom;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.open4goods.crawler.model.CustomUrlProvider;
import org.open4goods.crawler.services.fetching.WebDatasourceFetchingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

/**
 * curl -vik
 * 'https://www.sustainalytics.com/sustapi/companyratings/getcompanyratings' \
 * -H 'content-type: application/x-www-form-urlencoded; charset=UTF-8' \
 * --data-raw
 * 'industry=&rating=&filter=&page=1407&pageSize=10&resourcePackage=Sustainalytics'
 * 
 * 
 * 
 */
public class SustaynalyticsSeedExtractor implements CustomUrlProvider {

	RestTemplate restTemplate = new RestTemplate();
	private static final Logger logger = LoggerFactory.getLogger(SustaynalyticsSeedExtractor.class);

	@Override
	public Set<String> getUrls() {

		Set<String> ret = new HashSet<>();
		
		// TODO : Hard limit from conf
		for (int i =1; i < 2000; i++) {
			
			List<String> urls = getCompanyRatings(i);
			if (urls.size() == 0) {
				break;
			}
			ret.addAll(urls);			
		}

		return ret;
	}

	/**
	 * Return the urls of the company ratings
	 * @param page
	 * @return
	 */
	public List<String> getCompanyRatings(int page) {
		logger.info("Parsing sustainalytics seeds, page {}",page);
		List<String> ret = new ArrayList<>();

		String url = "https://www.sustainalytics.com/sustapi/companyratings/getcompanyratings";
		String body = "industry=&rating=&filter=&page=" + page + "&pageSize=100&resourcePackage=Sustainalytics";

		// Création de l'entête de la requête
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

		// Création de l'entité avec le corps de la requête et les entêtes
		HttpEntity<String> entity = new HttpEntity<>(body, headers);

		// Envoi de la requête POST
		ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

		if (null != response.getBody()) {
			String frags[] = response.getBody().split("data-href=\"");
	
			for (String frag : frags) {
				String urlFrag = frag.split("\"")[0];
				if (urlFrag.startsWith("/")) {
					ret.add("https://www.sustainalytics.com/esg-rating" + urlFrag);
				}
			}
	
			logger.info("Found {} urls", ret.size());
		}
		return ret;
	}

}
