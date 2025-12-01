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
            logger.error("Empty blabla (invalid expressions in template ?) generated for {} : {} > {}",
                    data != null ? data.gtin() : "<null>", input, xmlBlabla);
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

        if (StringUtils.isEmpty(text)) {
            return text;
        }

        final StringBuilder result = new StringBuilder(text.length());
        int cursor = 0;

        while (cursor < text.length()) {
            final int start = text.indexOf(RANDOM_START_TOKEN, cursor);
            if (start < 0) {
                result.append(text, cursor, text.length());
                break;
            }

            result.append(text, cursor, start);
            final int end = text.indexOf(RANDOM_END_TOKEN, start + RANDOM_START_TOKEN.length());
            if (end < 0) {
                logger.error("Was expecting an ending {} in BlablaExpression {}", RANDOM_END_TOKEN, text);
                result.append(text.substring(start));
                break;
            }

            final String[] choices = splitChoices(text.substring(start + RANDOM_START_TOKEN.length(), end));
            if (choices.length == 1) {
                if (seqGen.getNextAlea(2) == 1) {
                    result.append(choices[0]);
                }
            } else {
                result.append(choices[seqGen.getNextAlea(choices.length)]);
            }

            cursor = end + RANDOM_END_TOKEN.length();
        }
        return result.toString();
    }

    private String[] splitChoices(final String segment) {
        int start = 0;
        int index = segment.indexOf('|');
        if (index < 0) {
            return new String[]{segment};
        }

        final String[] buffer = new String[countSeparators(segment) + 1];
        int bufferIndex = 0;
        while (index >= 0) {
            buffer[bufferIndex++] = segment.substring(start, index);
            start = index + 1;
            index = segment.indexOf('|', start);
        }
        buffer[bufferIndex] = segment.substring(start);
        return buffer;
    }

    private int countSeparators(final String segment) {
        int count = 0;
        int idx = segment.indexOf('|');
        while (idx >= 0) {
            count++;
            idx = segment.indexOf('|', idx + 1);
        }
        return count;
    }
}
