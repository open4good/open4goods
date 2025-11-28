package org.open4goods.model.attribute;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

/**
 * Base class for attributes that track multiple sourced values.
 */
public abstract class SourcableAttribute {

        private static final List<String> DEFAULT_TRUSTED_SOURCE_PRIORITY = List.of("eprel", "icecat.biz");

        private static List<String> trustedSourcePriority = new ArrayList<>(DEFAULT_TRUSTED_SOURCE_PRIORITY);

        protected Set<SourcedAttribute> source = new HashSet<>();

        public boolean hasConflicts() {
                return distinctValues() > 1;
        }

        public String bgRow() {
                String ret = "table-default";
                int sCount = sourcesCount();
                long dValues = distinctValues();

                if (sCount == 0) {
                        ret = "table-danger";
                } else if (sCount == 1) {
                        ret = "table-default";
                } else {
                        ret = "table-info";
                }

                if (dValues > 1) {
                        ret = "table-danger";
                }

                return ret;
        }


        /**
         * Returns the most reliable value based on datasource priority, vote count and
         * lexical order. Values are normalized (trimmed, whitespace condensed and
         * lowercased) before counting so that variations such as "Noir" and "noir"
         * are considered equivalent. Tie-breaking is deterministic: datasource
         * priority wins first, then highest vote count, then lexical order of the
         * normalized value.
         *
         * @return the elected value, or {@code null} when no source provided a
         *         non-blank value
         */
        public String bestValue() {

                if (source.isEmpty()) {
                        return null;
                }

                Map<String, ValueStats> valueCounter = new HashMap<>();

                for (SourcedAttribute sourcedAttribute : source) {
                        if (StringUtils.isBlank(sourcedAttribute.getValue())) {
                                continue;
                        }

                        String normalizedValue = normalizeValue(sourcedAttribute.getValue());
                        int priorityIndex = trustedSourcePriorityIndex(sourcedAttribute.getDataSourcename());

                        valueCounter.computeIfAbsent(normalizedValue, ignored -> new ValueStats())
                                        .accept(priorityIndex, sourcedAttribute.getValue());
                }

                if (valueCounter.isEmpty()) {
                        return null;
                }

                Comparator<Map.Entry<String, ValueStats>> comparator = Comparator
                                .<Map.Entry<String, ValueStats>>comparingInt(entry -> entry.getValue().bestPriority)
                                .thenComparing(entry -> entry.getValue().count, Comparator.reverseOrder())
                                .thenComparing(Map.Entry::getKey);

                return valueCounter.entrySet().stream()
                                .min(comparator)
                                .map(entry -> entry.getValue().bestOriginalValue)
                                .orElse(null);
        }

        /**
         * Return the number of distinct values
         *
         * @return
         */
        public long ponderedvalues() {
                return source.stream().map(e -> e.getValue()).distinct().count();
        }


        /**
         * Number of sources for this attribute
         *
         * @return
         */
        public int sourcesCount() {
                return source.size();
        }

        /**
         * The number of different values for this item
         *
         * @return
         */
        public long distinctValues() {
                return source.stream().map(e->e.getValue()).distinct().count();
        }

        /**
         * For UI, a String representation of all providers names
         *
         * @return
         */
        public String providersToString() {
                return StringUtils.join(source.stream().map(e->e.getDataSourcename()).toArray(), ", ");
        }

        /**
         * For UI, a String representation of all providers names and values
         *
         * @return
         */
        public String sourcesToString() {
                return StringUtils.join(source.stream().map(e -> e.getDataSourcename() + ":" + e.getValue()).toArray(), ", ");

        }

        public Set<SourcedAttribute> getSource() {
                return source;
        }

        public void setSource(Set<SourcedAttribute> source) {
                this.source = source;
        }

        /**
         * Overrides the trusted source priority list. The order of the provided list
         * defines the priority (index 0 is the most trusted).
         *
         * @param priority ordered list of datasource names
         */
        public static void setTrustedSourcePriority(List<String> priority) {
                if (priority == null) {
                        trustedSourcePriority = new ArrayList<>();
                        return;
                }
                trustedSourcePriority = new ArrayList<>(priority);
        }

        /**
         * Resets the trusted source priority to the module default.
         */
        public static void resetTrustedSourcePriority() {
                trustedSourcePriority = new ArrayList<>(DEFAULT_TRUSTED_SOURCE_PRIORITY);
        }

        private int trustedSourcePriorityIndex(String datasource) {
                if (datasource == null) {
                        return Integer.MAX_VALUE;
                }
                Optional<Integer> index = java.util.stream.IntStream.range(0, trustedSourcePriority.size())
                                .filter(i -> datasource.equalsIgnoreCase(trustedSourcePriority.get(i)))
                                .boxed()
                                .findFirst();
                return index.orElse(Integer.MAX_VALUE);
        }

        private String normalizeValue(String value) {
                return StringUtils.normalizeSpace(value).trim().toLowerCase(Locale.ROOT);
        }

        private static final class ValueStats {
                private int count = 0;
                private int bestPriority = Integer.MAX_VALUE;
                private String bestOriginalValue = null;

                private void accept(int priority, String originalValue) {
                        count++;
                        String cleanedOriginal = StringUtils.normalizeSpace(originalValue);
                        if (bestOriginalValue == null || priority < bestPriority
                                        || (priority == bestPriority && cleanedOriginal.compareTo(bestOriginalValue) < 0)) {
                                bestPriority = priority;
                                bestOriginalValue = cleanedOriginal;
                        }
                }
        }
}
