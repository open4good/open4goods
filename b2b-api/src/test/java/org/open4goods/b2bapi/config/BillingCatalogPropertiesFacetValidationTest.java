package org.open4goods.b2bapi.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import org.junit.jupiter.api.Test;
import org.open4goods.b2bapi.config.BillingCatalogProperties.Facet;

/**
 * Bean Validation coverage for ADR 0017: an {@code eprel}-sourced facet must be
 * structurally free (0 credits, never billable).
 */
class BillingCatalogPropertiesFacetValidationTest {

    private static final Validator VALIDATOR;

    static {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            VALIDATOR = factory.getValidator();
        }
    }

    private Facet facet(final String source, final int credits, final String billableWhen) {
        final Facet facet = new Facet();
        facet.setPath("/api/v1/products/{gtin}/energy");
        facet.setDoc("products/energy");
        facet.setSource(source);
        facet.setCredits(credits);
        facet.setBillableWhen(billableWhen);
        return facet;
    }

    @Test
    void eprelFacetWithCreditsViolatesTheGuard() {
        final Set<ConstraintViolation<Facet>> violations = VALIDATOR.validate(facet("eprel", 10, "never"));

        assertThat(violations)
                .extracting(ConstraintViolation::getPropertyPath)
                .anyMatch(path -> path.toString().equals("eprelFacetStructurallyFree"));
    }

    @Test
    void eprelFacetWithCreditsAndWrongBillableWhenViolatesTheGuard() {
        final Set<ConstraintViolation<Facet>> violations = VALIDATOR.validate(facet("eprel", 10, "fresh-offer"));

        assertThat(violations)
                .extracting(ConstraintViolation::getPropertyPath)
                .anyMatch(path -> path.toString().equals("eprelFacetStructurallyFree"));
    }

    @Test
    void eprelFacetFreeAndNeverBillableIsValid() {
        final Set<ConstraintViolation<Facet>> violations = VALIDATOR.validate(facet("eprel", 0, "never"));

        assertThat(violations).noneMatch(v -> v.getPropertyPath().toString().equals("eprelFacetStructurallyFree"));
    }

    @Test
    void nonEprelFacetIsUnaffectedByTheGuard() {
        final Set<ConstraintViolation<Facet>> violations = VALIDATOR.validate(facet(null, 10, "fresh-offer"));

        assertThat(violations).noneMatch(v -> v.getPropertyPath().toString().equals("eprelFacetStructurallyFree"));
    }
}
