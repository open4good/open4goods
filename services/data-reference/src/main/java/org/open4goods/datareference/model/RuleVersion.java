package org.open4goods.datareference.model;

import java.util.Objects;
import java.util.regex.Pattern;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Version of a resolution, normalization or mapping rule.
 *
 * <p>Every derived value records the rule version that produced it, so a
 * projection can be rebuilt and explained without fetching providers again.
 *
 * @param ruleId stable rule identifier
 * @param version monotonic version of that rule
 */
public record RuleVersion(String ruleId, int version) {

    private static final Pattern RULE_ID_PATTERN = Pattern.compile("[a-z0-9]+(?:[.\\-][a-z0-9]+)*");

    /**
     * Validates the rule coordinates.
     */
    public RuleVersion {
        Objects.requireNonNull(ruleId, "ruleId must not be null");
        ruleId = ruleId.trim();
        if (!RULE_ID_PATTERN.matcher(ruleId).matches()) {
            throw new IllegalArgumentException(
                    "rule id must be lower-case alphanumeric separated by '.' or '-': " + ruleId);
        }
        if (version < 1) {
            throw new IllegalArgumentException("rule version must be strictly positive");
        }
    }

    /**
     * Returns the stable serialized form.
     *
     * @return identifier such as {@code quantity-normalization@3}
     */
    @JsonValue
    public String externalForm() {
        return ruleId + "@" + version;
    }

    /**
     * Rebuilds a rule version from its serialized form.
     *
     * @param value serialized form such as {@code quantity-normalization@3}
     * @return validated rule version
     */
    @JsonCreator
    public static RuleVersion parse(String value) {
        Objects.requireNonNull(value, "value must not be null");
        int separator = value.lastIndexOf('@');
        if (separator < 0) {
            throw new IllegalArgumentException("Rule version must be <ruleId>@<version>: " + value);
        }
        try {
            return new RuleVersion(value.substring(0, separator),
                    Integer.parseInt(value.substring(separator + 1)));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Rule version must be <ruleId>@<version>: " + value, exception);
        }
    }

    @Override
    public String toString() {
        return externalForm();
    }
}
