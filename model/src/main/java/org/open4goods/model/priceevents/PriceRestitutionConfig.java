package org.open4goods.model.priceevents;

import java.time.LocalDate;
import java.util.List;

import org.open4goods.model.Localisable;

import com.fasterxml.jackson.annotation.JsonFormat;

public class PriceRestitutionConfig {

    private Localisable<String, List<Event>> events = new Localisable<>();

    public Localisable<String, List<Event>> getEvents() {
        return events;
    }

    public void setEvents(Localisable<String, List<Event>> events) {
        this.events = events;
    }

    
}
