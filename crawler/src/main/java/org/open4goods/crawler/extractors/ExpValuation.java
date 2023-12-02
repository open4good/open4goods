package org.open4goods.crawler.extractors;

import java.lang.reflect.Method;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExpValuation {

	private static final Logger logger = LoggerFactory.getLogger(ExpValuation.class);

	public enum Operation {
		 uppercase, lowercase,
		 cdiscountCommentsValuationClassTransform,
		 ldlcCommentsValuationClassTransform,
		 ldlcCommentsNameValuationClassTransform,
		 cdiscountCommentsDate,
//		 cdiscountCommentsAuthor,
		 cdiscountQuestionAuthor,cdiscountQuestionDate,

		 fnacCommentDateParser,ecoguideParseRating,boulangerReviewCount, boulangerReviewValue
	}

	private static Map<String, Operation> operationNames = new HashMap<>();

	static {
		for (final Operation o : Operation.values()) {
			operationNames.put(o.toString().toLowerCase(), o);
		}
	}

	private String originalExpression;
	private String transformedExpression;
	private List<Operation> transformations = new ArrayList<>();

	public static ExpValuation from(final String expression) {
		final ExpValuation v = new ExpValuation();
		v.setOriginalExpression(expression);

		try {
			final String[] frags = expression.split("::");
			final StringBuilder s = new StringBuilder();

			int cur = 0;
			for (final String frag : frags) {
				cur++;
				final Operation o = operationNames.get(frag.trim().toLowerCase());
				if (null != o) {
					v.getTransformations().add(o);
				} else {
					s.append(frag);
					if (frags.length != cur) {
						s.append("::");
					}
				}
			}

			v.setTransformedExpression(s.toString());
		} catch (final Exception e) {
			logger.error("Cannot instanciate ExpValuation object from {} : {}",expression,e.getMessage());
		}

		return v;
	}

	public String apply(final String xpathEval) {
		String ret = xpathEval;
		for (final Operation o : transformations) {
			// String.class here is the parameter type, that might not be the
			// case with you
			// TODO(perf) :  caching
			// TODO(design): externalize "provider specific code" in dedicated classes, remove the enum
			try {
				final Method method = getClass().getMethod(o.toString(), String.class);
				ret = method.invoke(null, ret).toString();
			}  catch (final NoSuchMethodException e) {
				logger.error("The custom operation {} is not implemented ({})",o.toString(),e.getMessage());
			}catch (final Exception e) {
				logger.error("Unexpected error in expression evaluation of {} on operation {} :  {}",xpathEval, o.toString(),  e.getMessage());
			}
		}

		return ret;
	}

	//////////////////////////////////////////////////////////////////////////////
	// The list of supported methods. Are dynamically fetched from defined enum
	//////////////////////////////////////////////////////////////////////////////

	public static String uppercase(final String input) {
		return input.toUpperCase();
	}


	public static String lowercase(final String input) {
		return input.toLowerCase();
	}

	public static String ldlcCommentsValuationClassTransform(final String input) {
		return input.replace("star-","");
	}

	public static String ldlcCommentsNameValuationClassTransform(final String input) {

		if (input.startsWith("par ")) {
			return input.substring(4);
		}
		return input;
	}

	public static String cdiscountCommentsValuationClassTransform(final String input) {
		final int pos = input.toLowerCase().indexOf("stn");
		return input.substring(pos + 3, pos + 4);
	}

	public static String cdiscountCommentsDate(final String input) {
		if (StringUtils.isEmpty(input)) {
			return "";
		}

		try {
			return input.substring(input.lastIndexOf("le")+2);
		} catch (final Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return input;
	}

//	public static String cdiscountCommentsAuthor(final String input) {
//		return input.substring(0,input.indexOf(" a r") );
//	}


	public static String cdiscountQuestionAuthor(final String input) {
		return input.substring(input.indexOf(" par ") + 5,input.lastIndexOf(" le ") );
	}


	public static String cdiscountQuestionDate(final String input) {
		return input.substring(input.indexOf(" le ") + 4);
	}



	public static String fnacCommentDateParser(final String input) {
		return input.substring(input.indexOf("le")+3);
	}


	public static String boulangerReviewCount(final String input) {
		return input.substring(1,input.indexOf("avis")).trim();
	}

	public static String boulangerReviewValue(final String input) {
		final int pos = input.indexOf("star_");
		final char[] middle = input.substring(pos + 5,pos+7).toCharArray();

		return middle[0]+"."+middle[1];
	}





	//NOTE(gof) : Ugly, i don't know how to type
	private static final Map<Object, Object> letterScores = Collections.unmodifiableMap(Stream.of(
            new SimpleEntry<>("A+", "12"),
            new SimpleEntry<>("A-", "11"),
            new SimpleEntry<>("B+", "10"),
            new SimpleEntry<>("B-", "9"),
            new SimpleEntry<>("C+", "8"),
            new SimpleEntry<>("C-", "7"),
            new SimpleEntry<>("D+", "6"),
            new SimpleEntry<>("D−", "5"),
            new SimpleEntry<>("D-", "5"),
            new SimpleEntry<>("E+", "4"),
            new SimpleEntry<>("E−", "3"),
            new SimpleEntry<>("E-", "3"),
            new SimpleEntry<>("F+", "2"),
            new SimpleEntry<>("F-", "1"))
            .collect(Collectors.toMap(SimpleEntry::getKey, SimpleEntry::getValue)));

	public static String ecoguideParseRating(final String input) {

		final Object val = letterScores.get( StringEscapeUtils.unescapeHtml4(input).trim());
		if (null != val) {
			return val.toString();
		} else {
			logger.error("Cannot evaluate {} in method ExpValuation.ecoguideParseRating()",input);
			return input;
		}
	}




	////////////////////////////////////////////////////////////////////////////////////
	// Getters / setters
	////////////////////////////////////////////////////////////////////////////////////

	public String getOriginalExpression() {
		return originalExpression;
	}

	public void setOriginalExpression(final String originalExpression) {
		this.originalExpression = originalExpression;
	}

	public String getTransformedExpression() {
		return transformedExpression;
	}

	public void setTransformedExpression(final String transformedExpression) {
		this.transformedExpression = transformedExpression;
	}

	public static Map<String, Operation> getOperationNames() {
		return operationNames;
	}

	public static void setOperationNames(final Map<String, Operation> operationNames) {
		ExpValuation.operationNames = operationNames;
	}

	public List<Operation> getTransformations() {
		return transformations;
	}

	public void setTransformations(final List<Operation> transformations) {
		this.transformations = transformations;
	}

}