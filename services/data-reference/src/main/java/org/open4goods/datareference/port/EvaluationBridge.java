package org.open4goods.datareference.port;

import org.open4goods.datareference.model.projection.EvaluationInput;
import org.open4goods.datareference.model.projection.EvaluationSummary;

/**
 * Produces deterministic evaluation output from eligible projection inputs.
 *
 * <p>The evaluation implementation owns cohort-statistics computation. It
 * returns explicit missing inputs instead of consulting legacy {@code Product}
 * data, and uses the fixed instant and versions in {@link EvaluationInput} for
 * replayable results.
 */
public interface EvaluationBridge {

    /**
     * Evaluates one policy-filtered product input.
     *
     * @param input eligible values, offer summary and fixed replay coordinates
     * @return scores, cohort statistics and any missing inputs
     */
    EvaluationSummary evaluate(EvaluationInput input);
}
