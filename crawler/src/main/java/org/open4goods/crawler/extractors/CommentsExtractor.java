package org.open4goods.crawler.extractors;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.open4goods.config.yml.datasource.CommentsProperties;
import org.open4goods.config.yml.datasource.DataSourceProperties;
import org.open4goods.config.yml.datasource.ExtractorConfig;
import org.open4goods.crawler.services.fetching.DataFragmentWebCrawler;
import org.open4goods.model.Localised;
import org.open4goods.model.data.Comment;
import org.open4goods.model.data.DataFragment;
import org.open4goods.model.data.Rating;
import org.open4goods.model.data.RatingType;
import org.w3c.dom.Document;

import edu.uci.ics.crawler4j.crawler.CrawlController;
import edu.uci.ics.crawler4j.crawler.Page;
import edu.uci.ics.crawler4j.parser.HtmlParseData;

/**
 *
 * @author goulven
 *
 */
public class CommentsExtractor extends Extractor {

	private static final String EMPTY_USER_USER_NAME = "Inconnu";

	@Override
	public void parse(final String url, final Page page, final HtmlParseData parseData, final Document document,
                      final DataSourceProperties providerConfig,  final Locale locale,
                      final DataFragment p, final DataFragmentWebCrawler offerWebCrawler,final CrawlController crawlController) {

		final ExtractorConfig c = getExtractorConfig();

		final CommentsProperties cc = c.getCommentsProperties();

		if (null != cc) {

			final List<String> titles = evalMultipleAndLogs(document, cc.getTitle(), url);
			final List<String> descriptions = evalMultipleAndLogs(document, cc.getDescription(), url);
			final List<String> date = evalMultipleAndLogs(document, cc.getDate(), url);
			final List<String> author = evalMultipleAndLogs(document, cc.getAuthor(), url);
			final List<String> usefulls = evalMultipleAndLogs(document, cc.getUsefull(), url);
			final List<String> uselesss = evalMultipleAndLogs(document, cc.getUseless(), url);


			final List<String> ratings = unSplitRatings(evalMultipleAndLogs(document, cc.getRating().getValue(), url));

			if (titles.size() == descriptions.size() && descriptions.size() == date.size()
					&& date.size() == author.size() && author.size() == ratings.size()) {

				for (int i = 0; i < titles.size(); i++) {
					final Comment comment = new Comment();
					comment.setTitle(new Localised(titles.get(i), locale.getLanguage()));
					comment.setDescription(new Localised(descriptions.get(i), locale.getLanguage()));
					comment.setAuthor(StringUtils.isEmpty(author.get(i)) ? EMPTY_USER_USER_NAME : author.get(i));

					if (null != cc.getUseless()) {
						try {
							comment.setUseless(getUtility(uselesss.get(i), cc.getUseRemovals()));
						} catch (final Exception e) {
							getDedicatedLogger().warn("Error while parsing useless  : {} ; {}", e.getMessage(), url);
						}
					}

					if (null != cc.getUsefull()) {
						try {
							comment.setUsefull(getUtility(usefulls.get(i), cc.getUseRemovals()));
						} catch (final Exception e) {
							getDedicatedLogger().warn("Error while parsing usefull  : {} ; {}", e.getMessage(), url);
						}
					}

					try {
						comment.setDate(parseDate(date.get(i), providerConfig.getDateFormat(),providerConfig.getDatesPrefixesToRemove(),providerConfig.getDatesCutAt(),  locale));
					} catch (final Exception e) {
						getDedicatedLogger().warn("Error while parsing date {} : {} ; {}",date.get(i), e.getMessage(), url);
						continue;
					}
					final Rating rating = new Rating();
					rating.setMax(Double.valueOf( evalAndLogs(document, cc.getRating().getMax(), url)));
					rating.setMin(Integer.valueOf(evalAndLogs(document, cc.getRating().getMin(), url)));
					rating.setValue(Double.valueOf(ratings.get(i)));
					rating.addTag(RatingType.COMMENT);

					comment.setRating(rating);
					try {
						p.addComment(comment);
					} catch (final Exception e) {
						getDedicatedLogger().warn("Error while adding comment : {} ; {}", e.getMessage(), url);
					}
				}
			} else {
				getDedicatedLogger()
						.warn("Titles, descriptions, date, author, ratings do not have the same length. ; {}", url);
			}

		}

	}

	/**
	 * If rating is 4/5, keep only the 4
	 *
	 * @param evalMultipleAndLogs
	 * @return
	 */
	private List<String> unSplitRatings(final List<String> evalMultipleAndLogs) {
		final List<String> ret = new ArrayList<>();

		for (final String rating : evalMultipleAndLogs) {
			final String[] s = rating.split("/");
			ret.add(s[0]);
		}

		return ret;
	}

}
