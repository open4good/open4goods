package org.open4goods.datareference.model.grouping;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Static structural guard against the classic catastrophic-backtracking
 * (ReDoS) regex shape before a {@link ModelPatternRule} pattern is ever
 * matched against untrusted provider text.
 *
 * <p>A reviewed family pattern comes from Git, but the text it runs against is
 * provider-supplied and unbounded. This rejects a quantified group that itself
 * contains a quantified sub-expression -- the root cause of exponential
 * backtracking in patterns such as {@code (a+)+} or {@code ([a-z]+)*} -- at
 * authoring time, deterministically and without ever running the pattern
 * against a crafted worst-case input.
 */
final class CatastrophicRegexDetector {

    private CatastrophicRegexDetector() {
    }

    /**
     * Detects a nested-quantifier shape known to cause catastrophic backtracking.
     *
     * @param pattern raw regex source, as written by the rule author
     * @return {@code true} when a quantified group contains its own quantified
     *     sub-expression
     */
    static boolean isCatastrophic(String pattern) {
        Deque<boolean[]> groupContainsQuantifier = new ArrayDeque<>();
        boolean inClass = false;
        int length = pattern.length();
        for (int i = 0; i < length; i++) {
            char c = pattern.charAt(i);
            if (c == '\\') {
                i++;
                continue;
            }
            if (inClass) {
                if (c == ']') {
                    inClass = false;
                }
                continue;
            }
            if (c == '[') {
                inClass = true;
                continue;
            }
            if (c == '(') {
                groupContainsQuantifier.push(new boolean[] {false});
                continue;
            }
            if (c == ')') {
                boolean[] inner = groupContainsQuantifier.isEmpty() ? new boolean[] {false} : groupContainsQuantifier.pop();
                int quantifierSpan = repetitionSpan(pattern, i + 1);
                boolean groupRepeated = quantifierSpan > 0;
                if (groupRepeated && inner[0]) {
                    return true;
                }
                if (!groupContainsQuantifier.isEmpty() && (groupRepeated || inner[0])) {
                    groupContainsQuantifier.peek()[0] = true;
                }
                i += quantifierSpan;
                continue;
            }
            if ((c == '*' || c == '+') && !groupContainsQuantifier.isEmpty()) {
                groupContainsQuantifier.peek()[0] = true;
                continue;
            }
            if (c == '{') {
                int span = repetitionSpan(pattern, i);
                if (span > 0 && !groupContainsQuantifier.isEmpty()) {
                    groupContainsQuantifier.peek()[0] = true;
                    i += span - 1;
                }
            }
        }
        return false;
    }

    /**
     * Returns how many characters a repetition quantifier consumes starting at
     * {@code index}, or {@code 0} when there is none there.
     *
     * <p>{@code ?} is deliberately excluded: an optional sub-expression does not
     * itself multiply backtracking the way {@code *} or an open-ended
     * {@code {m,}}/{@code {m,n}} range does.
     */
    private static int repetitionSpan(String pattern, int index) {
        if (index >= pattern.length()) {
            return 0;
        }
        char c = pattern.charAt(index);
        if (c == '*' || c == '+') {
            return 1;
        }
        if (c != '{') {
            return 0;
        }
        int end = pattern.indexOf('}', index);
        if (end < 0) {
            return 0;
        }
        String spec = pattern.substring(index + 1, end);
        if (!spec.matches("\\d*,?\\d*") || spec.isEmpty() || spec.equals(",")) {
            return 0;
        }
        if (!spec.contains(",")) {
            // an exact count such as {3} bounds repetition by a constant factor,
            // not by the length of the input -- not the exponential shape this
            // guards against.
            return 0;
        }
        return end - index + 1;
    }
}
