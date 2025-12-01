package org.open4goods.commons.services.textgen;

import java.util.SplittableRandom;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Allows to get constant randomized aleas from a string identifier.
 */
public class BlaBlaSecGenerator {

    private final AtomicInteger counter = new AtomicInteger(0);
    private final SplittableRandom random;

    public BlaBlaSecGenerator(final Integer sourceHashCode) {
        this.random = new SplittableRandom(Integer.toUnsignedLong(sourceHashCode));
    }

    public int getNextAlea(final int boundExclusive) {
        if (boundExclusive <= 0) {
            throw new IllegalArgumentException("boundExclusive must be positive");
        }
        counter.incrementAndGet();
        return random.nextInt(boundExclusive);
    }

    public int getSequenceCount() {
        return counter.intValue();
    }

}
