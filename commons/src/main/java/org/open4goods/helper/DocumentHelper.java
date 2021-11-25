/**
 *
 */

package org.open4goods.helper;

import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.DomSerializer;
import org.htmlcleaner.HtmlCleaner;
import org.open4goods.exceptions.TechnicalException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * @author gof
 */
public class DocumentHelper {

	private static final Logger logger = LoggerFactory.getLogger(DocumentHelper.class);



	// create an instance of HtmlCleaner
	private static  HtmlCleaner htmlCleaner;

	private static  DomSerializer domSerializer;
	
	private static  Transformer transformer; 
	 

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
	 * Get a clean (using htmlCleaner) W3C document from an URL.
	 *
	 * @param string
	 * @return
	 * @throws java.text.ParseException
	 */
	public static Document getDocument(final String string) throws Exception {
		return domSerializer.createDOM(htmlCleaner.clean(string));

	}
	
	//method to convert Document to String
	public static String getStringFromDocument(Node node) throws TechnicalException
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
	

}
