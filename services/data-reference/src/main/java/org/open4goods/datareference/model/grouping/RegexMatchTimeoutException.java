package org.open4goods.datareference.model.grouping;

/**
 * Signals that a {@link java.util.regex.Matcher} running against untrusted
 * text was aborted because it exceeded its allotted time budget.
 *
 * <p>Thrown from inside {@link TimeBoundedCharSequence#charAt(int)} as the
 * matcher backtracks, so it carries no stack trace worth paying for: the
 * condition is expected whenever a pattern degenerates into exponential
 * backtracking, not an exceptional failure to diagnose.
 */
final class RegexMatchTimeoutException extends RuntimeException {

    RegexMatchTimeoutException() {
        super("regex match exceeded its time budget", null, false, false);
    }
}
