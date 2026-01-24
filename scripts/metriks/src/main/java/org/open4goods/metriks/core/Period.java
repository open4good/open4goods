package org.open4goods.metriks.core;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Represents the period used to compute metriks values.
 */
public final class Period
{

    private static final DateTimeFormatter DATE_KEY_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;
    private static final ZoneId PARIS_ZONE = ZoneId.of("Europe/Paris");

    private final int lastPeriodInDays;
    private final LocalDate startDate;
    private final LocalDate endDate;

    private Period(int lastPeriodInDays, LocalDate startDate, LocalDate endDate)
    {
        this.lastPeriodInDays = lastPeriodInDays;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    /**
     * Builds a period for the requested number of days.
     *
     * @param lastPeriodInDays number of days to look back
     * @param dateOverride end date override (Paris timezone), or null to use today
     * @return a new period instance
     */
    public static Period from(int lastPeriodInDays, LocalDate dateOverride)
    {
        LocalDate endDate = dateOverride != null ? dateOverride : LocalDate.now(PARIS_ZONE);
        LocalDate startDate = endDate.minusDays(lastPeriodInDays);
        return new Period(lastPeriodInDays, startDate, endDate);
    }

    public int lastPeriodInDays()
    {
        return lastPeriodInDays;
    }

    public LocalDate startDate()
    {
        return startDate;
    }

    public LocalDate endDate()
    {
        return endDate;
    }

    /**
     * Returns the date key format (YYYYMMDD) using the end date.
     *
     * @return date key string
     */
    public String dateKey()
    {
        return DATE_KEY_FORMATTER.format(endDate);
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Period period = (Period) o;
        return lastPeriodInDays == period.lastPeriodInDays
                && Objects.equals(startDate, period.startDate)
                && Objects.equals(endDate, period.endDate);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(lastPeriodInDays, startDate, endDate);
    }
}
