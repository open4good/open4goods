package org.open4goods.crawler.extractors.custom;

import java.util.Locale;

import org.open4goods.config.yml.datasource.DataSourceProperties;
import org.open4goods.crawler.extractors.Extractor;
import org.open4goods.crawler.services.fetching.DataFragmentWebCrawler;
import org.open4goods.exceptions.ValidationException;
import org.open4goods.model.Localised;
import org.open4goods.model.data.Comment;
import org.open4goods.model.data.DataFragment;
import org.open4goods.model.data.Rating;
import org.open4goods.model.data.RatingType;
import org.w3c.dom.Document;

import com.fasterxml.jackson.databind.JsonNode;
import com.mashape.unirest.http.Unirest;

import edu.uci.ics.crawler4j.crawler.CrawlController;
import edu.uci.ics.crawler4j.crawler.Page;
import edu.uci.ics.crawler4j.parser.HtmlParseData;

/**
 *
 * @author goulven
 * TODO(design) : one day, a generic bazaarvoice extracator
 */
public class BoulangerExtractor extends Extractor {

	private static final int LIMIT = 100;

	@Override
	public void parse(final String url, final Page page, final HtmlParseData parseData, final Document document,
                      final DataSourceProperties providerConfig, final Locale locale,
                      final DataFragment p, final DataFragmentWebCrawler offerWebCrawler,final CrawlController crawlController) {



		try {
			// Handling comments.. BazaarVoice is easy to browse .. No productId, get all the comments (gurps !)


			// Getting the product id (sku)
			//<a href="#tab-bazaarvoice" load-reviews-id="000000000001086076" >
			final String sku = evalAndLogs(document, "//a[@href='#tab-bazaarvoice']/@load-reviews-id", url);



			// Generating bazar voice base url


			// 000000000001086076&limit=100&Offset=135

			parseComments(sku, parseData, document, p,0);


			// Adding images
			final String ean = evalAndLogs(document, "//span[@itemprop='gtin13']", url);


//			List<String> imagesRawData = evalMultipleAndLogs(document, "//div[@class='s7thumb']/@style", url);
//
//			for (String raw : imagesRawData) {
//				System.out.println(raw);
//
//				int start = raw.indexOf(ean);
//				if (start == -1) {
//					continue;
//				}
//				String item = raw.substring(start, raw.indexOf("?",start));
//
//			}

			//TODO(gof) : not so nice
			final int max = 5;
			for (int i = 0; i < max; i++ ) {
				p.addResource("https://boulanger.scene7.com/is/image/Boulanger/"+ean+"_h_f_l_"+i+"?qlt=100,0&scl=0.5");
			}



		} catch (final Exception e) {
			getDedicatedLogger().error("Error while parsing comments : {}",url,e);
		}





	}




	// TODO(gof) : make a bazarvoice extractor
	private void parseComments(final String sku, final HtmlParseData parseData, final Document document,
			final DataFragment p, final Integer from) {

		// Retrieve the questions / answers url from JsVarExtractor
		final String commentsUrl = "https://api.bazaarvoice.com/data/reviews.json?apiversion=5.4&passkey=la29mmecb146hv5b0ynf5pnpx&filter=ProductId:"+sku+"&limit="+LIMIT+"&Offset="+from;



		final JsonNode root = getJsonRootNode(Unirest.get(commentsUrl).header("x-requested-with", "XMLHttpRequest"));
		// Evaluation through JsonPointers
		final Integer reviewsCount = root.get("Results").size();

		for (int i = 0; i < reviewsCount; i++) {

			final Comment c = new Comment();
			c.setAuthor(root.get("Results").get(i).get("UserNickname").asText());
			try {

				final Long d = Extractor.parseDate(root.get("Results").get(i).get("SubmissionTime").asText(), "yyyy-MM-dd'T'HH:mm:ss.SSSz", null,null,Locale.getDefault());

				c.setDate(d);
			} catch (final Exception e) {
				getDedicatedLogger().warn("Unable to parse date: {} ; {}",e.getMessage(),commentsUrl);
				continue;
			}

			final String lang = root.get("Results").get(i).get("ContentLocale").asText();
			c.setTitle(new Localised(root.get("Results").get(i).get("Title").asText(), lang));
			c.setDescription(new Localised(root.get("Results").get(i).get("ReviewText").asText(), lang));
			try {
				c.setUsefull(root.get("Results").get(i).get("TotalPositiveFeedbackCount").asInt());
			} catch (final Exception e1) {
				getDedicatedLogger().warn("Cannot parse usefull in : {}",commentsUrl);
			}
			try {
				c.setUseless(root.get("Results").get(i).get("TotalNegativeFeedbackCount").asInt());
			} catch (final Exception e1) {
				getDedicatedLogger().warn("Cannot parse useless in : {}",commentsUrl);
			}




			final Rating r = new Rating();
			r.addTag(RatingType.COMMENT);
			r.setMax(root.get("Results").get(i).get("RatingRange").asDouble());
			r.setValue(root.get("Results").get(i).get("Rating").asDouble());

			r.setMin(0);


			c.setRating(r);



			try {

				p.addComment(c);
			} catch (final ValidationException e) {
				getDedicatedLogger().warn("Unable to validate comment : {} ",e.getMessage());
			}
		}


		if (LIMIT == reviewsCount) {
			parseComments(sku, parseData, document, p, from+100);
		}
	}
}
