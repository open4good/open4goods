package org.open4goods.services;

import java.util.Map.Entry;

import org.open4goods.model.constants.ReferentielKey;
import org.open4goods.model.product.Product;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.StringTemplateResolver;

public class EvaluationService {

	private static final Logger logger = LoggerFactory.getLogger(EvaluationService.class);

	private static final String TPL_VAR_START = "${";
	private static final String TPL_VAR_STOP = "}";

	//	private final  SpringTemplateEngine thymeleafTemplateEngine;

	private static ExpressionParser expressionParser;

	private SpringTemplateEngine thymeleafTemplateEngine;

	public EvaluationService() {
		super();

		/////////////////////////
		// Thymeleaf engine initialisation
		/////////////////////////
				thymeleafTemplateEngine = new SpringTemplateEngine();
				final StringTemplateResolver templateResolver = new StringTemplateResolver();
				templateResolver.setTemplateMode(TemplateMode.TEXT);
//		TODO(conf,p2,0.25) : Cacheable from config (config.getWebConfig().getTemplatesCaching())
				templateResolver.setCacheable(true);

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

	/**
	 * Generate a name from the thymleaf template weared in conf
	 * Sample : "blabla de [(${p.vertical})]"
	 * TODO : Should raise exception if unresolvable variable
	 * @param p
	 * @param template
	 * @return
	 */
		public String thymeleafEval(final Product p, final String template) {
			/**
			 * Generate a name from the thymleaf template weared in conf
			 *
			 * @param p
			 * @param siteLocale
			 * @return
			 */
			try {
				final Context ctx = new Context();
				ctx.setVariable("data", p);
				ctx.setVariable("p", p);
				ctx.setVariable("product", p);
				
				// Adding referentiel keys
				for (Entry<ReferentielKey, String> e : p.getAttributes().getReferentielAttributes().entrySet()) {
					ctx.setVariable(e.getKey().toString(), e.getValue());
				}
	
	
				final String ret = thymeleafTemplateEngine.process(template, ctx);
				return ret;
	
			} catch (final Exception e) {
				logger.warn("Eval failed for {} :  {} : {}", p, e.getMessage(), e.getCause().getMessage());
				return null;
			}
		}
	//



}
