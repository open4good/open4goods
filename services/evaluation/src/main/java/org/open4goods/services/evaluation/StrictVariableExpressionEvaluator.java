package org.open4goods.services.evaluation;

import org.open4goods.services.evaluation.exception.TemplateEvaluationException;
import org.thymeleaf.context.IExpressionContext;
import org.thymeleaf.standard.expression.IStandardVariableExpression;
import org.thymeleaf.standard.expression.IStandardVariableExpressionEvaluator;
import org.thymeleaf.standard.expression.StandardExpressionExecutionContext;

/**
 * A custom variable expression evaluator that throws a TemplateEvaluationException
 * if a variable expression evaluates to null.
 */
public class StrictVariableExpressionEvaluator implements IStandardVariableExpressionEvaluator {

    private final IStandardVariableExpressionEvaluator delegate;

    /**
     * Constructs a StrictVariableExpressionEvaluator wrapping the given delegate.
     *
     * @param delegate the default variable expression evaluator.
     */
    public StrictVariableExpressionEvaluator(IStandardVariableExpressionEvaluator delegate) {
        this.delegate = delegate;
    }

	@Override
	public Object evaluate(IExpressionContext context, IStandardVariableExpression expression, StandardExpressionExecutionContext expContext) {
		// TODO Auto-generated method stub
		Object result = delegate.evaluate(context, expression, expContext);
		if (result == null) {
			throw new TemplateEvaluationException("Unresolved variable in expression: " + expression);
		}
		return result;
	}
}
