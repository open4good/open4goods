package org.open4goods.crawler.extractors;

import java.util.List;
import java.util.Locale;

import javax.xml.xpath.XPathExpressionException;

import org.open4goods.config.yml.datasource.DataSourceProperties;
import org.open4goods.config.yml.datasource.ExtractorConfig;
import org.open4goods.config.yml.datasource.QuestionsConfig;
import org.open4goods.crawler.services.fetching.DataFragmentWebCrawler;
import org.open4goods.exceptions.ResourceNotFoundException;
import org.open4goods.exceptions.ValidationException;
import org.open4goods.model.Localised;
import org.open4goods.model.data.Answer;
import org.open4goods.model.data.DataFragment;
import org.open4goods.model.data.Question;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import edu.uci.ics.crawler4j.crawler.CrawlController;
import edu.uci.ics.crawler4j.crawler.Page;
import edu.uci.ics.crawler4j.parser.HtmlParseData;

/**
 *
 * @author goulven
 *
 */
public class QuestionsExtractor extends Extractor {

	@Override
	public void parse(final String url, final Page page, final HtmlParseData parseData, final Document document,
                      final DataSourceProperties providerConfig, final Locale locale,
                      final DataFragment p, final DataFragmentWebCrawler offerWebCrawler,final CrawlController crawlController) {

		final ExtractorConfig c = getExtractorConfig();

		final QuestionsConfig cc = c.getQuestionsConfig();

		if (null != cc) {

			// Getting the root block
			NodeList root;
			try {
				root = xpathMultipleEval(document, cc.getRoot());
			} catch (final XPathExpressionException e) {

				getDedicatedLogger().error("Cannot evaluate ROOT XPATH for question : {} > {}", cc.getRoot(), e);
				return;
			}


			for (int i = 0; i < root.getLength(); i++) {
				try {
					final Question q = new Question();

					final Node rootNode = root.item(i);
					try {
						q.setTitle(new Localised(xpathEval(rootNode, cc.getQuestionTitle()),
								locale.getLanguage()));
					} catch (final ResourceNotFoundException e) {
						getDedicatedLogger().info("Cannot evaluate ROOT XPATH TITLE for question : {} > {}",
								cc.getQuestionTitle(), e.getMessage());
						continue;
					}

					if (null != cc.getQuestionDate()) {
						try {
							q.setDate(parseDate(xpathEval(rootNode, cc.getQuestionDate()),
									providerConfig.getDateFormat(),providerConfig.getDatesPrefixesToRemove(), providerConfig.getDatesCutAt(), locale));
						} catch (final Exception e) {
							getDedicatedLogger().error("Error while adding question date : {} > {}", cc.getRoot(), e.getMessage());						}
					}

					if (null != cc.getQuestionAuthor()) {
						try {
							q.setAuthor(xpathEval(rootNode, cc.getQuestionAuthor()));
						} catch (ResourceNotFoundException e) {
							getDedicatedLogger().info("Cannot evaluate ROOT XPATH AUTHOR for question : {} > {}",
									cc.getQuestionAuthor(), e.getMessage());
							continue;
						}
					}


					List<String> answersAuthor = null, answersTitle = null, answersDate = null, answersUsefull = null, answersUseless = null;
					// Checking now for answers

					Integer varianceCheckSize = null;
					if (null != cc.getAnswerAuthor()) {
						answersAuthor = evalMultipleAndLogs(rootNode, cc.getAnswerAuthor(), url);
						if (null == varianceCheckSize) {
							varianceCheckSize = answersAuthor.size();
						}
					}
					if (null != cc.getAnswerTitle()) {
						answersTitle = evalMultipleAndLogs(rootNode, cc.getAnswerTitle(), url);
						if (null == varianceCheckSize) {
							varianceCheckSize = answersTitle.size();
						} else {
							if (varianceCheckSize != answersTitle.size()) {
								getDedicatedLogger().warn("Not the expected size for answersTitle");
								continue;
							}
						}
					}

					if (null != cc.getAnswerDate()) {
						answersDate = evalMultipleAndLogs(rootNode, cc.getAnswerDate(), url);
						if (null == varianceCheckSize) {
							varianceCheckSize = answersDate.size();
						} else {
							if (varianceCheckSize != answersDate.size()) {
								getDedicatedLogger().warn("Not the expected size for answersDate");
								continue;
							}
						}
					}

					if (null != cc.getAnswerUsefull()) {
						answersUsefull = evalMultipleAndLogs(rootNode, cc.getAnswerUsefull(), url);
						if (null == varianceCheckSize) {
							varianceCheckSize = answersUsefull.size();
						} else {
							if (varianceCheckSize != answersUsefull.size()) {
								getDedicatedLogger().warn("Not the expected size for answersUsefull");
								continue;
							}
						}
					}

					if (null != cc.getAnswerUseless()) {
						answersUseless = evalMultipleAndLogs(rootNode, cc.getAnswerUseless(), url);
						if (null == varianceCheckSize) {
							varianceCheckSize = answersUseless.size();
						} else {
							if (varianceCheckSize != answersUseless.size()) {
								getDedicatedLogger().warn("Not the expected size for answersUseless");
								continue;
							}
						}
					}






					if (null != answersTitle) {
						for (int j = 0; j < answersTitle.size(); j++) {
							final Answer a = new Answer();
							a.setAnswer(new Localised(answersTitle.get(j), locale.getLanguage()));

							if (null != answersAuthor) {
								a.setAuthor(answersAuthor.get(j));
							}

							if (null != answersDate) {
								try {
									a.setDate(parseDate(answersDate.get(j), providerConfig.getDateFormat(),providerConfig.getDatesPrefixesToRemove(), providerConfig.getDatesCutAt(), locale));
								} catch (final ValidationException e) {
									getDedicatedLogger().error("Error while validating answer : {} > {}", cc.getRoot(), e.getMessage());
								}
							}

							if (null != answersUsefull) {
								try {
									a.setUsefull(getUtility(answersUsefull.get(j),cc.getUseRemovals()));
								} catch (final NumberFormatException e) {
									getDedicatedLogger().warn("Cannot parse to usefull answer : {} ; {}",answersUsefull.get(j),url);
								}
							}

							if (null != answersUseless) {
								try {
									a.setUseless(getUtility(answersUseless.get(j),cc.getUseRemovals()));
								} catch (final NumberFormatException e) {
									getDedicatedLogger().warn("Cannot parse to useless answer : {} ; {}",answersUseless.get(j),url);
								}
							}

							try {
								a.getAnswer().validate();
								q.getAnswers().add(a);
							} catch (final ValidationException e) {
								getDedicatedLogger().warn("Invalid answer : {} ", url);
							}
						}

					}

					if (q.getAnswers().size() > 0) {
						try {
							p.addQuestion(q);
						} catch (final ValidationException e) {
							getDedicatedLogger().error("Error while adding question : {} > {}", cc.getRoot(), e.getMessage());
						}
					} else {
						getDedicatedLogger().info("No answers for question at {}",url);
					}

				} catch (final XPathExpressionException e) {

					getDedicatedLogger().error("Cannot evaluate IN BLOCK XPATH for answer of : {} > {}", cc.getRoot(),
							e);
					return;
				}

			}

		}

	}



}
