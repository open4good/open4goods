package org.open4goods.crawler.extractors;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.lang3.StringUtils;
import org.open4goods.config.yml.datasource.DataSourceProperties;
import org.open4goods.config.yml.datasource.ExtractorConfig;
import org.open4goods.crawler.services.fetching.DataFragmentWebCrawler;
import org.open4goods.exceptions.ResourceNotFoundException;
import org.open4goods.exceptions.ValidationException;
import org.open4goods.helper.GenericFileLogger;
import org.open4goods.helper.IdHelper;
import org.open4goods.model.data.DataFragment;
import org.open4goods.services.EvaluationService;
import org.open4goods.services.SerialisationService;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.expression.EvaluationException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.fasterxml.jackson.databind.JsonNode;
import com.mashape.unirest.request.GetRequest;

import ch.qos.logback.classic.Level;
import edu.uci.ics.crawler4j.crawler.CrawlController;
import edu.uci.ics.crawler4j.crawler.Page;
import edu.uci.ics.crawler4j.parser.HtmlParseData;
//TODO(design) : ugly class, do multiple things (xpath / sanitisation, ...)
public abstract class Extractor {

	//TODO(conf) : logs folder from conf
	protected static Logger parserLogger = GenericFileLogger.initLogger("string-parsers", Level.WARN, "./capsule-data/logs/", false);


	private ExtractorConfig extractorConfig;


	protected @Autowired Environment env;

	protected @Autowired ApplicationContext applicationContext;

	protected @Autowired EvaluationService evaluationService;


	private static final TransformerFactory transfac = TransformerFactory.newInstance();

	private static final XPath xpath = XPathFactory.newInstance().newXPath();

	private static final Map<String, XPathExpression> xpathCache = new HashMap<>();

	private static final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

	private static  DocumentBuilder builder;


	//TODO(design) : Refactor extractors and associated configs. Will also fix injection concerns
	protected  SerialisationService serialisationService = new SerialisationService();

	private Logger dedicatedLogger;


	static {
		try {
			builder = factory.newDocumentBuilder();
		} catch (final ParserConfigurationException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Instanciate an extractor
	 *
	 * @param p
	 * @param conf
	 * @param sanitizer
	 * @param statsLogger
	 * @return
	 * @throws ClassNotFoundException
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 */
	public static Extractor getInstance( final ExtractorConfig conf, final Logger dedicatedLogger)
			throws ClassNotFoundException, InstantiationException, IllegalAccessException {

		final Class<? extends Extractor> extractorClass = (Class<? extends Extractor>) Class.forName(conf.getClassName());
		final Extractor extractor = extractorClass.newInstance();

		extractor.setExtractorConfig(conf);

		extractor.setDedicatedLogger(dedicatedLogger);
		// Set-up dedicated logger



		return extractor;
	}


	/**
	 * The crawlController, if needed to to direct fetchings
	 */
	private CrawlController crawlController;

	private final static Map<String, DateTimeFormatter> cacheDtf = new HashMap<>();

	public abstract void parse(String url, Page page, HtmlParseData parseData, Document document, DataSourceProperties conf, Locale locale, DataFragment p, DataFragmentWebCrawler offerWebCrawler,CrawlController controller);

	public ExtractorConfig getExtractorConfig() {
		return extractorConfig;
	}

	public void setExtractorConfig(final ExtractorConfig extractorConfig) {
		this.extractorConfig = extractorConfig;
	}


	public Logger getDedicatedLogger() {
		return dedicatedLogger;
	}

	public void setDedicatedLogger(final Logger dedicatedLogger) {
		this.dedicatedLogger = dedicatedLogger;
	}

	public String extractJsVar(final HtmlParseData parseData, final Document document, final String varName,
			final String url, final String equalSign) {
		final List<String> scripts = evalMultipleAndLogs(document, "//script", url);

		for (final String script : scripts) {

			Integer pos = script.indexOf(varName);
			if (pos != -1) {
				pos = script.indexOf(equalSign, pos);

				if (pos != -1) {
					Integer start = script.indexOf('"', pos);
					if (-1 == start) {
						start = script.indexOf('\'', pos);
					}

					if (-1 != start) {
						start++;
						Integer end = script.indexOf('"', start);
						if (-1 == end) {
							end = script.indexOf('\'', start);
						}
						if (-1 != end) {
							return script.substring(start, end);
						}

					}

				}
			}

		}

		return null;
	}

	/**
	 * Gets an external json
	 * @param request
	 * @return
	 */
	protected JsonNode getJsonRootNode(final GetRequest request) {
		try {
			return  serialisationService.getJsonMapper().readTree(request.asJson().getBody().toString());
		} catch (final Exception e) {
			throw new EvaluationException(e.getMessage());
		}
	}


	/**
	 * Do the evaluation and log accordingly
	 *
	 * @param document
	 * @param expression
	 * @return
	 */
	protected String evalAndLogs(final Node document, String expression, final String url) {

		if (StringUtils.isEmpty(expression)) {
			return null;
		}

		//TODO(perf) : could uidMap on expressions
		if (!expression.startsWith("/") && !expression.startsWith(".") && !expression.contains("::")) {
			// A fixed value
			return expression;
		}

		// Evaluating fixed expressions
		final String ret = evalFixedValue(expression);
		if (null != ret) {
			return ret;
		}

		final ExpValuation eval = ExpValuation.from(expression);
		expression = eval.getTransformedExpression();

		try {
			return IdHelper.sanitize(eval.apply(xpathEval(document, expression)));

		} catch (final XPathExpressionException e) {
			getDedicatedLogger().warn("xpath evaluation error;  {} ; {} ; {}", expression, e.getMessage(), url);
		} catch (final ResourceNotFoundException e) {
			getDedicatedLogger().info("xpath no match found ; {} ; {} ", expression, e.getMessage(), url);
		} catch (final Exception e) {
			getDedicatedLogger().error("xpath unexpected exception ; {} ; {} ", expression, e.getMessage(), url, e);
		}
		return null;
	}


	protected List<String> evalMultipleAndLogs(final Node document, final String expression, final String url) {
		return evalMultipleAndLogs(document, expression, url,true);
	}
	//TODO(design) : sanitisation also for evalSimple
	protected List<String> evalMultipleAndLogs(final Node document, String expression, final String url, final Boolean sanitize) {
		final List<String> ret = new ArrayList<>();
		if (StringUtils.isEmpty(expression)) {
			return null;
		}

		final ExpValuation eval = ExpValuation.from(expression);
		expression = eval.getTransformedExpression();

		try {

			final NodeList nodes = xpathMultipleEval(document, expression);

			for (int i = 0; i < nodes.getLength(); i++) {
				//TODO(gof) : could infer splittertags
				String res = eval.apply(extractContentWithCariageReturns(nodes.item(i),null));

				if (sanitize) {
					res = IdHelper.sanitize(res);
				}
				ret.add(res);
			}

		} catch (final XPathExpressionException e) {
			getDedicatedLogger().warn("xpath evaluation error;  {} ; {} ; {}", expression, e.getMessage(), url);
		} catch (final Exception e) {
			getDedicatedLogger().error("xpath unexpected exception ; {} ; {} ", expression, e.getMessage(), url, e);
		}
		return ret;
	}

	/**
	 * Upgraded version of jsonpointer eval that allow :<br/>
	 * /last/ >> points the last element of an array
	 *
	 * @param node
	 * @param expression
	 * @return
	 */
	public static String jsonEval(final JsonNode node, String expression) {
		if (StringUtils.isEmpty(expression)) {
			return null;
		}
		// Evaluating fixed expressions
		String ret = evalFixedValue(expression);
		if (null != ret) {
			return ret;
		}

		final ExpValuation eval = ExpValuation.from(expression);


		expression = eval.getTransformedExpression();

		final StringBuilder builder = new StringBuilder();
		final String[] frags = expression.split("/last/");
		int i = 0;
		for (final String frag : frags) {
			i++;
			builder.append(frag);
			if (i < frags.length) {
				final String size = String.valueOf(node.at(builder.toString()).size() - 1);
				builder.append("/").append(size).append("/");
			}
		}

		ret = node.at(builder.toString()).asText();
		return IdHelper.sanitize(eval.apply(ret));

	}

	private static String evalFixedValue(String expression) {
		// Checking for fixed value

		if (StringUtils.isEmpty(expression)) {
			return null;
		}
		if (StringUtils.isNumeric(expression)) {
			return expression;
		}

		expression = expression.trim();
		if ((expression.startsWith("'") || expression.startsWith("\""))
				&& (expression.endsWith("'") || expression.endsWith("\""))) {
			return expression.substring(1, expression.length() - 1);
		}
		return null;
	}


	/**
	 * Extract text content from a node. Will add \n if text found in multiple block at the root level
	 * @param value
	 * @return
	 */
	protected static String extractContentWithCariageReturns(final Node value, final Set<String> splitterTags) {

		// If empty splitter tags, return at one shot
		if (splitterTags != null && splitterTags.size() == 0) {
			return value.getTextContent();
		}

		int elems = 0;
		final StringBuilder sb = new StringBuilder();
		for (int i =0; i < value.getChildNodes().getLength(); i++) {
			// If no splitter tags, all are used.
			if (splitterTags == null) {
				elems++;
				sb.append(value.getChildNodes().item(i).getTextContent().trim()).append("\n");
			} else {

				if (splitterTags.contains( value.getChildNodes().item(i).getNodeName())) {
					sb.append("\n");
					elems++;
				} else {
					sb.append(value.getChildNodes().item(i).getTextContent().trim());
				}

			}
			}

		if (elems < 1) {
			return value.getTextContent() ;
		} else {
			final String ret =  sb.toString();
			if (ret.endsWith("\n")) {
				return ret .substring(0, ret.length()-1);
			} else {
				return ret;
			}
		}

	}


	/**
	 * Method that parse date using cached dateTimeFormatters
	 * @param date
	 * @param format
	 * @param locale
	 * @return
	 * @throws ValidationException
	 */
	public static Long parseDate(final String date, final String format, final Set<String> prefixesToRemove,final String cutAfter, final Locale locale) throws ValidationException {



		if (StringUtils.isEmpty(date)) {
			throw new ValidationException("Date is empty");
		}
		if (StringUtils.isEmpty(format)) {
			throw new ValidationException("Format is empty");
		}

		String value = date;

		if (!cacheDtf.containsKey(format)) {
			cacheDtf.put(format, DateTimeFormatter.ofPattern(format, locale));
		}

		try {

			// Removing prefixes
			if (null != prefixesToRemove) {
				for (final String p : prefixesToRemove) {
					if (value.startsWith(p)) {
						value = value.substring(p.length());
					}
				}
			}


			// Cutting at

			if (!StringUtils.isEmpty(cutAfter)) {
				final int pos = value.indexOf(cutAfter);
				if (-1 != pos) {
					value = value.substring(0,pos);
				}
			}

			LocalDate ldate;
			try {
				ldate = LocalDate.parse(value, cacheDtf.get(format));
			} catch (final DateTimeParseException e) {
				// Fallback on the lowercase version, because of monthes names.
				ldate = LocalDate.parse(value.toLowerCase(), cacheDtf.get(format));
			}
			return ldate.atStartOfDay(ZoneId.systemDefault()).toEpochSecond();
		} catch (final Exception e) {
			throw new ValidationException("Cannot parse date " + date + " with format " + format + " > " + e.getMessage());
		}

	}




	/**
	 * PErforms a simple XPATH evaluation upon a W3C document
	 *
	 * @param document
	 * @param expression
	 * @return
	 * @throws XPathExpressionException
	 */
	public static String xpathEval(final Node document,  String expression)
			throws XPathExpressionException, ResourceNotFoundException {


		final ExpValuation eval = ExpValuation.from(expression);
		expression = eval.getTransformedExpression();




		XPathExpression expr = xpathCache.get(expression);
		if (null == expr) {
			expr = xpath.compile(expression);
			xpathCache.put(expression, expr);
		}

		final NodeList nl = (NodeList) expr.evaluate(document, XPathConstants.NODESET);
		// Benchmarker.log("Xpath eval done", b,3);
		if (nl.getLength() > 1) {
			throw new XPathExpressionException(nl.getLength() + " element in xpath evaluation result");
		} else if (nl.getLength() == 0) {
			throw new ResourceNotFoundException("No results");
		}
		//TODO(feature) : could infer splitter tag
		return eval.apply(extractContentWithCariageReturns(nl.item(0),null));

	}



	/**
	 * PErforms a simple XPATH evaluation upon a W3C document
	 *
	 * @param document
	 * @param expression
	 * @return
	 * @throws XPathExpressionException
	 */
	public static NodeList xpathMultipleEval(final Node document, final String expression)
			throws XPathExpressionException {

		XPathExpression expr = xpathCache.get(expression);
		if (null == expr) {
			expr = xpath.compile(expression);
			xpathCache.put(expression, expr);
		}

		final NodeList nl = (NodeList) expr.evaluate(document, XPathConstants.NODESET);

		return nl;

	}


	/**
	 * PErforms a simple XPATH evaluation upon a W3C document
	 *
	 * @param document
	 * @param expression
	 * @return
	 * @throws XPathExpressionException
	 */
	public static List<String> xpathMultipleEvalAsString(final Node document, final String expression)
			throws XPathExpressionException {

		final List<String> ret = new ArrayList<>();

		final NodeList nodes = xpathMultipleEval(document, expression);
		for (int i = 0; i < nodes.getLength(); i++) {
			ret.add(nodes.item(i).getTextContent());
		}
		return ret;

	}


	/**
	 * Removes token if needed and parse as integer
	 * @param string
	 * @param useRemovals
	 * @return
	 * @throws NumberFormatException
	 */
	protected Integer getUtility(final String string, final List<String> useRemovals) throws NumberFormatException{


		String ret = string;
		for (final String rm : useRemovals) {
			ret = ret.replace(rm, "");
		}

		return  Integer.valueOf(StringUtils.normalizeSpace(ret));
	}

	public CrawlController getCrawlController() {
		return crawlController;
	}

	public void setCrawlController(final CrawlController crawlController) {
		this.crawlController = crawlController;
	}



}
