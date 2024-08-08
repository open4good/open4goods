/**
 *
 */

package org.open4goods.helper;

import java.io.StringReader;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.DomSerializer;
import org.htmlcleaner.HtmlCleaner;
import org.open4goods.exceptions.ResourceNotFoundException;
import org.open4goods.exceptions.TechnicalException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * @author gof
 */
public class DocumentHelper {

	private static final Logger logger = LoggerFactory.getLogger(DocumentHelper.class);


	static DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    static DocumentBuilder builder;
	   
	// create an instance of HtmlCleaner
	private static  HtmlCleaner htmlCleaner;

	private static  DomSerializer domSerializer;

	private static  Transformer transformer;

	private static final XPath xpath = XPathFactory.newInstance().newXPath();

	private static final Map<String, XPathExpression> xpathCache = new ConcurrentHashMap<>();



	static {
		logger.info("Initialising html cleaner");
		// create an instance of HtmlCleaner
		try {
			final CleanerProperties p = new CleanerProperties();
			p.setAllowInvalidAttributeNames(true);
			// p.setDeserializeEntities(true);
			// p.setTranslateSpecialEntities(true);

			htmlCleaner = new HtmlCleaner(p);

			domSerializer = new DomSerializer(p);

			TransformerFactory tf = TransformerFactory.newInstance();
			transformer = tf.newTransformer();
			
			builder =  factory.newDocumentBuilder();
		} catch (Exception e) {
			logger.error("Cannot instanciate xpath factory",e);
		}
	}

	/**
	 * Return the base url for a given URL
	 *
	 * @param url
	 * @return
	 */
	public static String getBaseUrl(final String url) {

		final StringBuilder builder = new StringBuilder();
		try {
			final URL uUrl = new URL(url);
			builder.append(uUrl.getProtocol()).append("://").append(uUrl.getHost())
			.append(uUrl.getPort() == -1 ? "" : ":" + uUrl.getPort()).append("/");
		} catch (final MalformedURLException e) {
			logger.error("Error while handling URL : {} ", url, e);
		}
		return builder.toString();
	}

	/**
	 * Get a clean (using htmlCleaner) W3C document from an string content.
	 *
	 * @param string
	 * @return
	 * @throws java.text.ParseException
	 */
	public static Document cleanAndGetDocument(final String string) throws Exception {
		return domSerializer.createDOM(htmlCleaner.clean(string));

	}
	
	/**
	 * Get a  W3C document from a string content.
	 *
	 * @param string
	 * @return
	 * @throws java.text.ParseException
	 */
	public static Document getDocument(final String string) throws Exception {
		    InputSource is = new InputSource(new StringReader(string));
		   return  builder.parse(is);
		    

	}

	//	//method to convert Document to String
	public static String getSourceFromDocument(Node node) throws TechnicalException
	{
		try
		{
			DOMSource domSource = new DOMSource(node);
			StringWriter writer = new StringWriter();
			StreamResult result = new StreamResult(writer);

			transformer.transform(domSource, result);
			return writer.toString();
		}
		catch(Exception e)
		{
			throw new TechnicalException("Xpath evaluation fail ",e);
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
		return nl.item(0).getTextContent();

	}


}
