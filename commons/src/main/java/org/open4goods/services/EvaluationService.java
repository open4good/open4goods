package org.open4goods.services;

import org.open4goods.model.product.Product;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
public class EvaluationService {

	private static final Logger logger = LoggerFactory.getLogger(EvaluationService.class);

	private static final String TPL_VAR_START = "${";
	private static final String TPL_VAR_STOP = "}";

	//	private final  SpringTemplateEngine thymeleafTemplateEngine;

	private static ExpressionParser expressionParser;

	public EvaluationService() {
		super();

		/////////////////////////
		// Thymeleaf engine initialisation
		/////////////////////////
		//		thymeleafTemplateEngine = new SpringTemplateEngine();
		//		final StringTemplateResolver templateResolver = new StringTemplateResolver();
		//		templateResolver.setTemplateMode(TemplateMode.TEXT);
		//TODO(conf,p2,0.25) : Cacheable from config (config.getWebConfig().getTemplatesCaching())
		//		templateResolver.setCacheable(true);

		//		thymeleafTemplateEngine.setTemplateResolver(templateResolver);

		///////////////////////////////
		// Spel engine initialisation
		///////////////////////////////
		expressionParser  = new SpelExpressionParser();
	}


	/**
	 * Validate a product against a Spring expression language query
	 *
	 * @param p
	 * @param spel
	 * @return
	 */
	public Boolean spelEval(final Product p, final String spel) {
		final Expression expression = expressionParser.parseExpression(spel);
		try {
			return expression.getValue(p, Boolean.class);
		} catch (final EvaluationException e) {
			logger.warn("Error while evaluating SpEl {} : {}", spel, e.getMessage());
			return false;
		}
	}

	/**
	 * Validate a product against a Spring expression language query
	 *
	 * @param p
	 * @param spel
	 * @return
	 */
	public String spelCompute(final Product p, final String spel) {
		final Expression expression = expressionParser.parseExpression(spel);
		try {
			return expression.getValue(p, String.class);
		} catch (final EvaluationException e) {
			logger.warn("Error while evaluating SpEl {} : {}", spel, e.getMessage());
			return "";
		}
	}


	//	public String thymeleafEval(final Product p, final String template) {
	//		/**
	//		 * Generate a name from the thymleaf template weared in conf
	//		 *
	//		 * @param p
	//		 * @param siteLocale
	//		 * @return
	//		 */
	//		try {
	//			final Context ctx = new Context();
	//			ctx.setVariable("data", p);
	//			ctx.setVariable("p", p);
	//			ctx.setVariable("product", p);
	//
	//			// Adding referentiel keys
	//			for (Entry<ReferentielKey, String> e : p.getAttributes().getReferentielAttributes().entrySet()) {
	//				ctx.setVariable(e.getKey().toString(), e.getValue());
	//			}
	//
	//
	//			final String ret = thymeleafTemplateEngine.process(template, ctx);
	//			return ret;
	//
	//		} catch (final RuntimeException e) {
	//			logger.warn("Eval failed for {} :  {} : {}", p, e.getMessage(), e.getCause().getMessage());
	//			return null;
	//		}
	//	}
	//




	/**
	 * Var table replacement. Our format is :
	 *
	 *
	 *
	 * TODO (feature,p1,0.5) : Add the following var in blablaservice
	 * datas, comments,proscons,attribute-coverage : NONE,FEW,MEDIUM,LOT
	 *
	 * MIN_PRICE
	 * MAX_PRICE
	 * AVG_PRICE
	 *
	 * DESCRIPTIONS_COUNT
	 * PROS_COUNT
	 * CONS_COUNT
	 *
	 * @param var
	 * @param data
	 * @param blablaContext
	 * @return
	 */
	private String replaceVar(String var, Product data) {

		String[] frags = var.trim().toUpperCase().split("\\.");

		if (frags.length == 1) {
			switch (frags[0]) {
			case "BRAND":
				return data.brand();
			case "UID":
			case "MODEL":
			case "BRANDUID":
			case "BRAND_UID":
				return data.model();
			default:
				logger.warn("Var {} is unknown", var);
				return "!!${" + var + "}!!";
			}

			////////////////////////////////////////////
			// Simple form :
			// -------------------------------
			// NAME
			//
			// [ATTRIBUTENAME]
			////////////////////////////////////////////
		} else if (frags.length == 2) {
			// TODO : Implement min, max, sum, others...
			logger.warn("Unhandled var size : {}", var);
		} else {
			logger.warn("Unhandled var size : {}", var);
		}

		return var;
	}
}
