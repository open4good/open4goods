package org.open4goods.datareference.model.grouping;

/**
 * Wraps a {@link CharSequence} so that a {@link java.util.regex.Matcher}
 * reading through it aborts once a fixed time budget elapses, instead of
 * running unbounded against pathological input.
 *
 * <p>{@link java.util.regex.Matcher} has no built-in timeout. It does,
 * however, re-read the subject text through {@link #charAt(int)} on every
 * character it visits, including every backtracking step. Checking the
 * deadline on each access turns that into a cooperative abort from inside the
 * matching engine itself -- no second thread or external interruption needed
 * -- by throwing {@link RegexMatchTimeoutException} once the budget is spent.
 */
final class TimeBoundedCharSequence implements CharSequence {

    private final CharSequence delegate;
    private final long deadlineNanos;

    TimeBoundedCharSequence(CharSequence delegate, long budgetNanos) {
        this.delegate = delegate;
        this.deadlineNanos = System.nanoTime() + budgetNanos;
    }

    private TimeBoundedCharSequence(CharSequence delegate, Deadline deadline) {
        this.delegate = delegate;
        this.deadlineNanos = deadline.nanos();
    }

    @Override
    public int length() {
        return delegate.length();
    }

    @Override
    public char charAt(int index) {
        if (System.nanoTime() > deadlineNanos) {
            throw new RegexMatchTimeoutException();
        }
        return delegate.charAt(index);
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        return new TimeBoundedCharSequence(delegate.subSequence(start, end), new Deadline(deadlineNanos));
    }

    @Override
    public String toString() {
        return delegate.toString();
    }

    /** Carries an already-computed absolute deadline, as opposed to a budget still to resolve against "now". */
    private record Deadline(long nanos) {
    }
}
