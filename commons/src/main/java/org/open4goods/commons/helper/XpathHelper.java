package org.open4goods.commons.helper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.open4goods.commons.exceptions.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
public class XpathHelper {

	//	private static final TransformerFactory transfac = TransformerFactory.newInstance();

	private static final XPath xpath = XPathFactory.newInstance().newXPath();

	private static final Map<String, XPathExpression> xpathCache = new HashMap<>();

	private static final Logger logger = LoggerFactory.getLogger(XpathHelper.class);



	/**
	 * PErforms a simple XPATH evaluation upon a W3C document
	 *
	 * @param document
	 * @param expression
	 * @return
	 * @throws XPathExpressionException
	 */
	public static Node xpathEval(final Node document,  String expression)
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
		return nl.item(0);

	}

	
	public static List<String> xpathMultipleEvalString(Node doc, String string) throws XPathExpressionException, ResourceNotFoundException {
		return xpathMultipleEval(doc, string).stream().map(e->e.getTextContent()).toList();
	}
	
	
	
	/**
	 * PErforms a simple XPATH evaluation upon a W3C document
	 *
	 * @param document
	 * @param expression
	 * @return
	 * @throws XPathExpressionException
	 */
	public static List<Node> xpathMultipleEval(final Node document,  String expression)
			throws XPathExpressionException, ResourceNotFoundException {

		
		List<Node> ret = new ArrayList<>();
		XPathExpression expr = xpathCache.get(expression);
		if (null == expr) {
			expr = xpath.compile(expression);
			xpathCache.put(expression, expr);
		}

		final NodeList nl = (NodeList) expr.evaluate(document, XPathConstants.NODESET);
		// Benchmarker.log("Xpath eval done", b,3);
	
		for (int i = 0; i < nl.getLength(); i++) {
			ret.add(nl.item(i));
		}
		
		return ret;


	}



}
