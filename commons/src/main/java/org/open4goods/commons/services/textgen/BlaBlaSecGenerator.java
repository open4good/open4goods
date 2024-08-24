package org.open4goods.commons.services.textgen;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Allows to get constant randomized aleas from a string identifier
 * @author goulven
 *
 */
public class BlaBlaSecGenerator {

	private final Integer sourceHashCode;
	private final AtomicInteger counter = new AtomicInteger(1);


	public BlaBlaSecGenerator(final Integer sourceHashCode) {
		super();
		this.sourceHashCode = sourceHashCode;
	}


	public int getNextAlea(final int max) {
		final Integer alea = (sourceHashCode+max) / counter.getAndIncrement();
		return Math.abs(alea % (max+1));
	}


	public int getSequenceCount() {
		return counter.intValue();
	}

}