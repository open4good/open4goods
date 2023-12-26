package org.open4goods.api.services.aggregation.services.realtime;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.open4goods.api.services.aggregation.AbstractRealTimeAggregationService;
import org.open4goods.helper.IdHelper;
import org.open4goods.model.data.DataFragment;
import org.open4goods.model.product.Names;
import org.open4goods.model.product.Product;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NamesAggregationService extends AbstractRealTimeAggregationService {

	private static final Logger logger = LoggerFactory.getLogger(NamesAggregationService.class);

	private static final int LAST_WORD_MIN_LENGTH = 2;

	private static final int NUMBER_OF_WORDS_TO_DEDUPLICATE = 3;


	public NamesAggregationService(final String logsFolder,boolean toConsole) {
		super(logsFolder,toConsole);
	}

	@Override
	public void onDataFragment(final DataFragment df, final Product output) {

		//		for (final Entry<String, String> tpl : localisationAggregationConfig.getProductNameTemplates().entrySet()) {
		//
		////			final String name = getProductName(output, tpl.getKey());
		//			final String name = getProductName(output, tpl.getKey());
		//
		//			names.getNames().put(tpl.getKey(), name);
		//			names.getOfferNames().add(name);
		//		}

		//		names.setName(output.name());

		// Adding raw offer names
		// TODO : names are not localized
		output.getNames().getOfferNames().addAll(df.getNames().stream()
				.map(this::normalizeName).collect(Collectors.toSet()));

		output.getNames().setName(URLEncoder.encode(computeProductName(output), StandardCharsets.UTF_8));


	}


	private String computeProductName(Product output) {
		
		String ret;
		if (!StringUtils.isEmpty(output.model()) && !StringUtils.isEmpty(output.brand())) {
			ret = output.gtin()+"-"+output.brand()+"-"+output.model();
		} else {
			ret = computeProductName(output.getNames(),output.gtin());;
		}
		
		
		return ret;
	}

	private String computeProductName(Names names, String gtin) {

		if (names.getOfferNames().size() == 0) {
			return gtin;
		}
		else {
			String[] frags = names.shortestOfferName().split(" ");

			int maxchars = 70;
			int curChars = 0;
			StringBuilder builder = new StringBuilder();

			LinkedHashSet<String> set = new LinkedHashSet<>();

			for (String f : frags) {
				if ((curChars + f.length()) > maxchars) {
					break;
				}

				String token = IdHelper.getUrlName(f);
				if (token.endsWith("-")) {
					token = token.substring(0,token.length()-1);
				}
				if (token.startsWith("-")) {
					token = token.substring(1);
				}

				if (!StringUtils.isBlank(token)) {
					set.add(token);
					curChars += token.length();
				}

				//			String token = f;
				//			builder.append(token);
			}

			builder.append(gtin).append("-");
			builder.append(StringUtils.join(set,"-"));


			return builder.toString();
		}
	}


	private String normalizeName( String name) {
		return name.trim();
	}
	
//	private String normalizeName(String name) {
//
//		String normalized = StringUtils.normalizeSpace(name.toLowerCase());
//
//		String[] frags = normalized.split(" ");
//
//		LinkedHashSet<String> linkedHashSet = new LinkedHashSet<>();
//		StringBuilder ret = new StringBuilder();
//
//		for (int i = 0; i < frags.length; i++) {
//
//			// Special case on the last word
//			if (i == frags.length - 1) {
//				if (frags[i].length() < LAST_WORD_MIN_LENGTH) {
//					continue;
//				}
//				if (frags[i].endsWith("...")) {
//					continue;
//				}
//
//			}
//
//			if (i <= NUMBER_OF_WORDS_TO_DEDUPLICATE) {
//				// Dedup this word
//				linkedHashSet.add(frags[i].toLowerCase());
//			} else {
//				ret.append(frags[i]).append(" ");
//
//			}
//
//		}
//		String result = (StringUtils.join(linkedHashSet, " ") + " " + ret.toString()).trim();
//
//		return result;
//	}

	//	public String getProductName(final Product p, final String language) {
	//		String template = null;
	//		try {
	//			template = localisationAggregationConfig.getProductNameTemplates().getOrDefault(language,
	//					localisationAggregationConfig.getProductNameTemplates().get("default"));
	//			return evaluationService.thymeleafEval(p, template);
	//
	//		} catch (final Exception e) {
	//			dedicatedLogger.error("Unable to generate name for {} with template {}. Error is : {}", p, template,
	//					e.getMessage());
	//		}
	//		return null;
	//	}

}
