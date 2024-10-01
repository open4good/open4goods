package org.open4goods.commons.services.textgen;

import org.apache.commons.lang3.StringUtils;
import org.open4goods.commons.exceptions.InvalidParameterException;
import org.open4goods.commons.model.product.Product;
import org.open4goods.commons.services.EvaluationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BlablaService {


	private static final String REGEX_ESCAPED_SPLIT_TOKEN = "\\|";
	private static final String RANDOM_START_TOKEN = "||";
	private static final String RANDOM_END_TOKEN = "||";

	private static final Logger logger = LoggerFactory.getLogger(BlablaService.class);

	private final EvaluationService evaluationService;


	public BlablaService(final EvaluationService evaluationService) {
		super();
		this.evaluationService = evaluationService;

	}

	/**
	 * 
	 * The format :
	 * 
	 * bonjour ||monsieur | mec|| vous avez ici une
	 * <block if="attr.PRIX.value > 200"> télévison assez chère.</block>
	 * <block if="attr.PRIX.value"> Plutôt <<correcte en terme de prix |
	 * abordable>></block>
	 * 
	 * 
	 * TODO(p2,design,0.25) : monitore performances
	 * 
	 * @param xmlBlabla
	 * @param hash
	 * @param data
	 * @return
	 */
	public String generateBlabla(String input, final Product data) throws InvalidParameterException {

		//////////////////////////////////
		// Aleas computation;
		/////////////////////////////////
		if (StringUtils.isEmpty(input)) {
			throw new InvalidParameterException("Null input");
		}

		String xmlBlabla = input;
		
		Long hash = Long.valueOf(xmlBlabla.hashCode())
				+ ((null == data) ? 0L : data.getId());
		final BlaBlaSecGenerator seqGen = new BlaBlaSecGenerator(hash.hashCode());

		logger.debug("generating blabla {}:{} >> {}", seqGen.getSequenceCount(), seqGen.hashCode(), xmlBlabla);

		//////////////////////////////////
		// Fast or
		/////////////////////////////////

		logger.debug("generating fastor {}:{} >> {}", seqGen.getSequenceCount(), seqGen.hashCode(), xmlBlabla);
		xmlBlabla = fastOr(xmlBlabla, seqGen);

	
		///////////////////////
		// Thymeleaf full templating
		////////////////////////
		xmlBlabla = evaluationService.thymeleafEval(data, xmlBlabla);
		logger.info("generating thymeleaf version {}:{} >> {}", seqGen.getSequenceCount(), seqGen.hashCode(), xmlBlabla);
		
		if (null == xmlBlabla) {
			logger.error("Empty blabla (invalid expressions in template ?) generated for {} : {} > {}",data.gtin(), input, xmlBlabla);
			return "";
		}
		return StringUtils.normalizeSpace(xmlBlabla);
	}

	

	/**
	 * Compute the nominal form from a "'fast or" string (<< choix1 || choix2 >>)
	 */
	public String fastOr(final String text, final BlaBlaSecGenerator seqGen) {

		// Choose the text from the alea
		String raw = text;

		// Compute the possible "inline" replacements
		int indexStart = raw.indexOf(RANDOM_START_TOKEN);

		while (indexStart != -1) {
			final StringBuilder ret = new StringBuilder(raw.length());
			final int indexStop = raw.indexOf(RANDOM_END_TOKEN, indexStart + RANDOM_END_TOKEN.length());
			if (-1 == indexStop) {
				logger.error("Was expecting an ending {} in BlablaExpression {}", RANDOM_END_TOKEN, raw);
				return raw;
			}

			ret.append(raw.substring(0, indexStart));

			final String[] choices = raw.substring(indexStart + RANDOM_START_TOKEN.length(), indexStop)
					.split(REGEX_ESCAPED_SPLIT_TOKEN);

			// case no split char : Yes / no mode
			if (1 == choices.length) {
				// append
				if (1 == seqGen.getNextAlea(1)) {
					ret.append(choices[0]);
				}
			} else {
				ret.append(choices[seqGen.getNextAlea(choices.length - 1)]);
			}

			ret.append(raw.substring(indexStop + RANDOM_END_TOKEN.length()));

			raw = ret.toString();

			indexStart = raw.indexOf(RANDOM_START_TOKEN);
		}
		return raw;
	}
}