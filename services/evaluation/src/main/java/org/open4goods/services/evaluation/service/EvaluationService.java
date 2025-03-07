package org.open4goods.services.evaluation.service;

import java.util.Map;
import java.util.Map.Entry;

import org.open4goods.model.product.Product;
import org.open4goods.services.evaluation.StrictSpringStandardDialect;
import org.open4goods.services.evaluation.config.EvaluationConfig;
import org.open4goods.services.evaluation.exception.TemplateEvaluationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.StringTemplateResolver;

/**
 * Service for evaluating products using Spring Expression Language (SpEL) and Thymeleaf templates.
 *
 * <p>
 * This service provides methods to evaluate conditions on products and generate computed strings based on product properties.
 * </p>
 */
@Service
public class EvaluationService {

    private static final Logger logger = LoggerFactory.getLogger(EvaluationService.class);

    // Constants for template variable markers (used for checking unresolved variables)
    private static final String TPL_VAR_START = "${";

    // SpEL expression parser (trusted expressions)
    private static ExpressionParser expressionParser;

    // Thymeleaf template engine
    private SpringTemplateEngine thymeleafTemplateEngine;

    /**
     * Constructs a new EvaluationService and initializes the template engines.
     *
     * @param evaluationProperties the evaluation properties injected from configuration.
     */
    public EvaluationService(EvaluationConfig evaluationProperties) {
        super();

        // Initialize Thymeleaf engine with custom strict dialect for variable evaluation
        thymeleafTemplateEngine = new SpringTemplateEngine();
        final StringTemplateResolver templateResolver = new StringTemplateResolver();
        templateResolver.setTemplateMode(TemplateMode.TEXT);
        // Set caching based on external configuration read from YAML
        templateResolver.setCacheable(evaluationProperties.isCacheable());
        thymeleafTemplateEngine.setTemplateResolver(templateResolver);
        thymeleafTemplateEngine.setDialect(new StrictSpringStandardDialect());

        // Initialize SpEL engine (expressions are trusted)
        expressionParser = new SpelExpressionParser();
    }

    /**
     * Evaluates a Spring Expression Language (SpEL) expression against the given product.
     *
     * @param p    the product to evaluate against.
     * @param spel the SpEL expression.
     * @return the boolean result of the evaluation.
     */
    public Boolean spelEval(final Product p, final String spel) {
        final Expression expression = expressionParser.parseExpression(spel);
        try {
            // Evaluate the expression on the product and return the result as Boolean.
            return expression.getValue(p, Boolean.class);
        } catch (final EvaluationException e) {
            // Log critical error and return false.
            logger.error("Critical error while evaluating SpEL '{}': {}", spel, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Computes a string value by evaluating a Spring Expression Language (SpEL) expression against the given product.
     *
     * @param p    the product to evaluate against.
     * @param spel the SpEL expression.
     * @return the computed string result of the evaluation.
     */
    public String spelCompute(final Product p, final String spel) {
        final Expression expression = expressionParser.parseExpression(spel);
        try {
            // Evaluate the expression on the product and return the result as String.
            return expression.getValue(p, String.class);
        } catch (final EvaluationException e) {
            // Log critical error and return an empty string.
            logger.error("Critical error while computing SpEL '{}': {}", spel, e.getMessage(), e);
            return "";
        }
    }

    /**
     * Evaluates a Thymeleaf template using the product data.
     *
     * @param p        the product to evaluate against.
     * @param template the Thymeleaf template.
     * @return the result of the template evaluation.
     * @throws TemplateEvaluationException if the template evaluation fails due to unresolved variables.
     */
    public String thymeleafEval(final Product p, final String template) {
        return thymeleafEval(p, template, null);
    }

    /**
     * Evaluates a Thymeleaf template using provided parameters.
     *
     * @param params   the map of parameters for template evaluation.
     * @param template the Thymeleaf template.
     * @return the result of the template evaluation.
     * @throws TemplateEvaluationException if the template evaluation fails due to unresolved variables.
     */
    public String thymeleafEval(final Map<String, Object> params, final String template) {
        return thymeleafEval(null, template, params);
    }

    /**
     * Evaluates a Thymeleaf template using the product data and additional parameters.
     *
     * @param p                the product to evaluate against.
     * @param template         the Thymeleaf template.
     * @param additionalParams additional parameters to be passed to the template.
     * @return the result of the template evaluation.
     * @throws TemplateEvaluationException if the template evaluation fails due to unresolved variables.
     */
    public String thymeleafEval(final Product p, final String template, Map<String, Object> additionalParams) {
        try {
            // Create the Thymeleaf context and add default variables.
            final Context ctx = new Context();
            ctx.setVariable("data", p);
            ctx.setVariable("p", p);
            ctx.setVariable("product", p);
            if (additionalParams != null) {
                ctx.setVariables(additionalParams);
            }

            // If a product is provided, add referential attributes to the context.
            if (p != null) {
                for (Entry<?, ?> e : p.getAttributes().getReferentielAttributes().entrySet()) {
                    ctx.setVariable(e.getKey().toString(), e.getValue());
                }
            }

            // Process the template with the provided context.
            // With the custom dialect in place, if any variable is unresolvable, a TemplateEvaluationException will be thrown.
            return thymeleafTemplateEngine.process(template, ctx);

        } catch (final Exception e) {
            // Log error and rethrow as TemplateEvaluationException for critical failures.
            logger.error("Template evaluation failed for template '{}': {}", template, e.getMessage(), e);
            throw new TemplateEvaluationException("Template evaluation failed", e);
        }
    }
}
