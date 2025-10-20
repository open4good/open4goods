package org.open4goods.nudgerfrontapi.service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.open4goods.model.Localisable;
import org.open4goods.model.priceevents.Event;
import org.open4goods.nudgerfrontapi.config.properties.PriceRestitutionProperties;
import org.open4goods.nudgerfrontapi.dto.event.CommercialEventDto;
import org.open4goods.nudgerfrontapi.localization.DomainLanguage;
import org.springframework.stereotype.Service;

/**
 * Service responsible for mapping configured commercial events to DTOs consumed
 * by the REST API.
 */
@Service
public class CommercialEventService {

    private final PriceRestitutionProperties priceRestitutionProperties;

    public CommercialEventService(PriceRestitutionProperties priceRestitutionProperties) {
        this.priceRestitutionProperties = priceRestitutionProperties;
    }

    /**
     * Retrieve the commercial events configured for the requested domain
     * language.
     *
     * @param domainLanguage requested language hint
     * @return commercial events converted to DTOs
     */
    public List<CommercialEventDto> commercialEvents(DomainLanguage domainLanguage) {
        return resolveEvents(domainLanguage).stream()
                .filter(Objects::nonNull)
                .map(this::toDto)
                .toList();
    }

    private List<Event> resolveEvents(DomainLanguage domainLanguage) {
        Localisable<String, List<Event>> localisable = priceRestitutionProperties.getEvents();
        if (localisable == null || localisable.isEmpty()) {
            return List.of();
        }

        List<Event> events = localisable.i18n(languageKey(domainLanguage));
        if (events != null) {
            return events;
        }

        if (domainLanguage != null) {
            events = localisable.get(domainLanguage.name());
            if (events != null) {
                return events;
            }
        }

        events = localisable.get("default");
        if (events != null) {
            return events;
        }

        Optional<List<Event>> firstNonNull = localisable.values().stream()
                .filter(Objects::nonNull)
                .findFirst();
        return firstNonNull.orElse(List.of());
    }

    private String languageKey(DomainLanguage domainLanguage) {
        return domainLanguage != null ? domainLanguage.languageTag() : null;
    }

    private CommercialEventDto toDto(Event event) {
        return new CommercialEventDto(event.getLabel(), event.getStartDate(), event.getEndDate(), event.getColor());
    }
}
