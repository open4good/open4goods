package org.open4goods.commons.services.textgen;

import org.apache.commons.lang3.StringUtils;
import org.open4goods.model.exceptions.InvalidParameterException;
import org.open4goods.model.product.Product;
import org.open4goods.services.evaluation.service.EvaluationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service responsible for generating deterministic marketing texts from a
 * template input. Optional variants are selected through {@link BlaBlaSecGenerator}
 * so the same hash always yields the same final text, while still providing
 * variation across different inputs or products.
 */
public class BlablaService {

    private static final String REGEX_ESCAPED_SPLIT_TOKEN = "\\|";
    private static final String RANDOM_START_TOKEN = "||";
    private static final String RANDOM_END_TOKEN = "||";

    private static final Logger logger = LoggerFactory.getLogger(BlablaService.class);

    private final EvaluationService evaluationService;

    /**
     * Creates a new instance of the service.
     *
     * @param evaluationService templating engine used for Thymeleaf evaluation
     */
    public BlablaService(final EvaluationService evaluationService) {
        super();
        this.evaluationService = evaluationService;

    }

    /**
     * Generates a normalized text from a template containing fast-or segments
     * and Thymeleaf expressions. The selection of alternative segments is backed
     * by {@link BlaBlaSecGenerator}, ensuring deterministic results for a given
     * combination of input text and product hash.
     * <p>
     * The expected format for alternatives is:
     * <pre>
     * bonjour ||monsieur | mec|| vous avez ici une
     * &lt;block if="attr.PRIX.value &gt; 200"&gt; télévison assez chère.&lt;/block&gt;
     * &lt;block if="attr.PRIX.value"&gt; Plutôt &lt;&lt;correcte en terme de prix | abordable&gt;&gt;&lt;/block&gt;
     * </pre>
     *
     * @param input the blabla template
     * @param data  contextual product data used for hashing and template evaluation
     * @return the generated, whitespace-normalized text
     * @throws InvalidParameterException if the input is empty or null
     */
    public String generateBlabla(String input, final Product data) throws InvalidParameterException {

        // Aleas computation;
        if (StringUtils.isEmpty(input)) {
            throw new InvalidParameterException("Null input");
        }

        String xmlBlabla = input;

        // Hash is based on template and product identifier to keep determinism per item
        Long hash = Long.valueOf(xmlBlabla.hashCode())
                        + ((null == data) ? 0L : data.getId());
        final BlaBlaSecGenerator seqGen = new BlaBlaSecGenerator(hash.hashCode());

        logger.debug("generating blabla {}:{} >> {}", seqGen.getSequenceCount(), seqGen.hashCode(), xmlBlabla);

        // Fast or replacement
        logger.debug("generating fastor {}:{} >> {}", seqGen.getSequenceCount(), seqGen.hashCode(), xmlBlabla);
        xmlBlabla = fastOr(xmlBlabla, seqGen);


        // Thymeleaf full templating
        xmlBlabla = evaluationService.thymeleafEval(data, xmlBlabla);
        logger.info("generating thymeleaf version {}:{} >> {}", seqGen.getSequenceCount(), seqGen.hashCode(), xmlBlabla);

        if (null == xmlBlabla) {
            logger.error("Empty blabla (invalid expressions in template ?) generated for {} : {} > {}",data.gtin(), input, xmlBlabla);
            return "";
        }
        return StringUtils.normalizeSpace(xmlBlabla);
    }



    /**
     * Resolves inline random alternatives contained in the given text. Segments
     * wrapped between the start and end token are split on {@code |} and the
     * deterministic {@link BlaBlaSecGenerator} decides which branch to keep.
     *
     * @param text   template containing inline alternatives
     * @param seqGen deterministic generator that controls the chosen alternative
     * @return the template with alternatives resolved
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