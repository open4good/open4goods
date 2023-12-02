package org.open4goods.crawler.extractors.custom;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;

import org.open4goods.config.yml.datasource.DataSourceProperties;
import org.open4goods.crawler.extractors.Extractor;
import org.open4goods.crawler.services.fetching.DataFragmentWebCrawler;
import org.open4goods.exceptions.ValidationException;
import org.open4goods.model.Localised;
import org.open4goods.model.data.Answer;
import org.open4goods.model.data.Comment;
import org.open4goods.model.data.DataFragment;
import org.open4goods.model.data.Question;
import org.open4goods.model.data.Rating;
import org.open4goods.model.data.RatingType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import com.fasterxml.jackson.databind.JsonNode;
import com.mashape.unirest.http.Unirest;

import edu.uci.ics.crawler4j.crawler.CrawlController;
import edu.uci.ics.crawler4j.crawler.Page;
import edu.uci.ics.crawler4j.parser.HtmlParseData;

/**
 *
 * @author goulven
 * TODO(design) : In a classical DeepExtractor
 * TODO(feature) : we don't have the vote useful/useless through API on answers
 *
 */
public class SonVideoExtractor extends Extractor {

	private final static Logger log = LoggerFactory.getLogger(SonVideoExtractor.class);

	private static final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ssX");

	@Override
	public void parse(final String url, final Page page, final HtmlParseData parseData, final Document document,
                      final DataSourceProperties providerConfig, final Locale locale,
                      final DataFragment p, final DataFragmentWebCrawler offerWebCrawler,final CrawlController crawlController) {


		try {
			// Handling questions
			final String questionsUrl = providerConfig.getWebDatasource().getBaseUrl()
					+ extractJsVar(parseData, document, "SonVideo.api.get_all_questions_data_url", url,"=");
			parseQuestions(questionsUrl, parseData, document, p);
		} catch (final Exception e) {
			getDedicatedLogger().error("Error while parsing questions : {}",url,e);
		}

		try {
			// Handling comments
			final String commentsUrl = providerConfig.getWebDatasource().getBaseUrl()
					+ extractJsVar(parseData, document, "SonVideo.api.get_reviews_url", url,"=");
			parseComments(commentsUrl, parseData, document, p);
		} catch (final Exception e) {
			getDedicatedLogger().error("Error while parsing comments : {}",url,e);
		}





	}

	private void parseQuestions(final String questionsUrl, final HtmlParseData parseData, final Document document,
			final DataFragment p) {
		// Retrieve the questions / answers url from JsVarExtractor


		final JsonNode root = getJsonRootNode(Unirest.get(questionsUrl).header("x-requested-with", "XMLHttpRequest"));
		// Evaluation through JsonPointers
		final int questionsCount = root.get("questions").size();

		for (int i = 0; i < questionsCount; i++) {

			final Question q = new Question();
			q.setAuthor(root.get("questions").get(i).get("username").asText());
			try {
				q.setDate(format.parse(root.get("questions").get(i).get("created_at").asText()).getTime());
			} catch (final ParseException e) {
				log.error("Error while parsing date : {} ; {}",e.getMessage(),questionsUrl);
			}

			final String lang = root.get("questions").get(i).get("supported_culture_id").asText();
			q.setTitle(new Localised(root.get("questions").get(i).get("title").asText(), lang));
			q.setDescription(new Localised(root.get("questions").get(i).get("question").asText(), lang));


			// Parsing the answers

			final int answersCount = root.get("questions").get(i).get("answers").size();

			for (int j = 0; j < answersCount; j++) {
				final Answer a = new Answer();

				a.setAnswer(new Localised(root.get("questions").get(i).get("answers").get(j).get("message").asText(),
						lang));
				a.setAuthor(root.get("questions").get(i).get("answers").get(j).get("user_id").asText());


				// TODO(feature) : the real date, after having a magical date parser
				// a.setDate(format.parse(root.get("questions").get(i).get("answers").get(j).get("created_at").asText()).getTime());
				a.setDate(q.getDate());


				q.getAnswers().add(a);

			}

			p.getQuestions().add(q);
		}
	}



	private void parseComments(final String url, final HtmlParseData parseData, final Document document,
			final DataFragment p) {
		// Retrieve the questions / answers url from JsVarExtractor


		final JsonNode root = getJsonRootNode(Unirest.get(url).header("x-requested-with", "XMLHttpRequest"));
		// Evaluation through JsonPointers
		final int reviewsCount = root.get("reviews").size();

		for (int i = 0; i < reviewsCount; i++) {

			final Comment c = new Comment();
			c.setAuthor(root.get("reviews").get(i).get("username").asText());
			try {
				c.setDate(format.parse(root.get("reviews").get(i).get("created_at").asText()).getTime());
			} catch (final ParseException e) {
				log.error("Error while parsing date : {} ; {}",e.getMessage(),url);
			}

			final String lang = root.get("reviews").get(i).get("supported_culture_id").asText();
			c.setTitle(new Localised(root.get("reviews").get(i).get("title").asText(), lang));
			c.setDescription(new Localised(root.get("reviews").get(i).get("message").asText(), lang));



			c.setUsefull(root.get("reviews").get(i).get("vote_useful").asInt(0));
			c.setUseless(root.get("reviews").get(i).get("vote_useless").asInt(0));

			final Rating r = new Rating();
			r.addTag(RatingType.COMMENT);
			//NOTE(gof) : HardCoded, but that's life... And it won't move.
			r.setMax(5.0);
			r.setValue(root.get("reviews").get(i).get("score").asDouble());
			r.setMin(0);

			c.setRating(r);




			try {
				p.addComment(c);
			} catch (final ValidationException e) {
				getDedicatedLogger().warn("Unable to validate comment : {} ",e.getMessage());
			}
		}
	}
}
