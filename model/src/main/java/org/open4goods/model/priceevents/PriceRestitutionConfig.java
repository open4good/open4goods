package org.open4goods.model.priceevents;

import java.util.List;

import org.open4goods.model.Localisable;

/**
 * Global configuration bean describing commercial events exposed to the
 * frontend.
 */
public class PriceRestitutionConfig {

    /** Localised list of commercial events keyed by language (e.g. {@code fr}). */
    private Localisable<String, List<Event>> events = new Localisable<>();

    /**
     * Return the configured commercial events grouped by language key.
     *
     * @return localisable collection of events, never {@code null}
     */
    public Localisable<String, List<Event>> getEvents() {
        return events;
    }

    /**
     * Update the configured commercial events.
     *
     * @param events localisable collection of events
     */
    public void setEvents(Localisable<String, List<Event>> events) {
        this.events = events != null ? events : new Localisable<>();
    }
}
