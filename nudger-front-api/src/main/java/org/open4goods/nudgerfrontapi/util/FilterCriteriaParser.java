package org.open4goods.nudgerfrontapi.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.open4goods.nudgerfrontapi.dto.filter.FilterCriteria;
import org.open4goods.nudgerfrontapi.dto.filter.FilterOperator;

/**
 * Utility to parse query parameters following the form {@code filter[field_op]=value}.
 */
public final class FilterCriteriaParser {

    private static final Pattern FILTER_PATTERN = Pattern.compile("filter\\[([a-zA-Z0-9_]+)\\]=(.+)");

    private FilterCriteriaParser() {
    }

    /**
     * Parse all parameters of the form {@code filter[field_op]=value} into {@link FilterCriteria} objects.
     *
     * @param parameters raw request parameters
     * @return list of parsed criteria
     */
    public static List<FilterCriteria> parse(Map<String, String[]> parameters) {
        List<FilterCriteria> result = new ArrayList<>();
        parameters.forEach((key, values) -> {
            Matcher m = FILTER_PATTERN.matcher(key);
            if (m.matches()) {
                String expr = m.group(1); // e.g. price_lt
                String[] parts = expr.split("_");
                String field = parts[0];
                FilterOperator op = parts.length > 1 ?
                        FilterOperator.valueOf(parts[1].toUpperCase()) : FilterOperator.EQ;
                String value = values.length > 0 ? values[0] : null;
                result.add(new FilterCriteria(field, op, value));
            }
        });
        return result;
    }
}
