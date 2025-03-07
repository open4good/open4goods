package org.open4goods.services.evaluation;

import org.thymeleaf.spring6.dialect.SpringStandardDialect;
import org.thymeleaf.standard.expression.IStandardVariableExpressionEvaluator;

/**
 * Custom Spring Standard Dialect that uses the StrictVariableExpressionEvaluator.
 */
public class StrictSpringStandardDialect extends SpringStandardDialect {

    @Override
    public IStandardVariableExpressionEvaluator getVariableExpressionEvaluator() {
        // Wrap the default evaluator provided by SpringStandardDialect with our strict evaluator.
        return new StrictVariableExpressionEvaluator(super.getVariableExpressionEvaluator());
    }
}
