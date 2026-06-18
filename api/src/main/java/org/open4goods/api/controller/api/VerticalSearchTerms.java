package org.open4goods.api.controller.api;

import java.util.LinkedHashSet;
import java.util.Set;

import org.open4goods.model.vertical.ProductI18nElements;
import org.open4goods.model.vertical.VerticalConfig;

/**
 * Shared helper for building the set of search terms derived from a {@link VerticalConfig}.
 * Used by {@link IcecatController} and {@link ReferentialHelperController} to score taxonomy
 * candidates against a vertical's localized names.
 */
final class VerticalSearchTerms {

    private VerticalSearchTerms() {
    }

    /**
     * Returns a deduplicated, insertion-ordered set of non-blank search terms for the vertical:
     * the vertical ID, its hyphen/underscore-unfolded form, and all localized names.
     */
    static Set<String> of(VerticalConfig vc) {
        Set<String> terms = new LinkedHashSet<>();
        addTerm(terms, vc.getId());
        if (vc.getId() != null) {
            addTerm(terms, vc.getId().replace('-', ' ').replace('_', ' '));
        }
        for (ProductI18nElements i18n : vc.getI18n().values()) {
            addTerm(terms, i18n.getCardName());
            addTerm(terms, i18n.getDisplayName());
            addTerm(terms, i18n.getPageTitle());
            addTerm(terms, i18n.getSeoName());
            addTerm(terms, i18n.getVerticalHomeTitle());
            addTerm(terms, i18n.getVerticalMetaTitle());
        }
        return terms;
    }

    static void addTerm(Set<String> terms, String term) {
        if (term != null && !term.isBlank()) {
            terms.add(term.trim());
        }
    }
}
