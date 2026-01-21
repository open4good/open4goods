package org.open4goods.model.util;

import java.util.Locale;

import org.joda.time.DurationFieldType;
import org.joda.time.Period;
import org.joda.time.PeriodType;
import org.joda.time.format.PeriodFormat;
import org.joda.time.format.PeriodFormatter;

/**
 * Shared utility to format "time ago" durations in a locale-aware way.
 */
public final class TimeAgoFormatter {

	private TimeAgoFormatter() {
	}

	/**
	 * Formats a duration in milliseconds into a localized, human-friendly string.
	 *
	 * @param locale   preferred locale (falls back to {@link Locale#getDefault()} when null)
	 * @param duration duration in milliseconds
	 * @return localized duration label (e.g. "2 days", "5 minutes")
	 */
	public static String formatDuration(Locale locale, long duration) {
		Locale resolvedLocale = locale != null ? locale : Locale.getDefault();
		Period period;

		if (duration < 3_600_000) {
			DurationFieldType[] min = { DurationFieldType.minutes(), DurationFieldType.seconds() };
			period = new Period(duration, PeriodType.forFields(min)).normalizedStandard();
		} else {
			DurationFieldType[] full = { DurationFieldType.days(), DurationFieldType.hours() };
			period = new Period(duration, PeriodType.forFields(full)).normalizedStandard();
		}

		PeriodFormatter formatter = PeriodFormat.wordBased(resolvedLocale);
		return formatter.print(period);
	}
}
