package org.open4goods.b2bapi.config;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Proves the EPREL zero-rate rule is a structural, fail-fast config constraint: a
 * {@code b2b-catalog.yml} entry claiming {@code source: eprel} while carrying a price is rejected
 * before the application can serve it, regardless of who edits the YAML in the future.
 */
class BillingCatalogPropertiesFacetValidationTest {

    private final ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory();
    private final Validator validator = validatorFactory.getValidator();

    @Test
    void acceptsFreeEprelSourcedFacet() {
        final BillingCatalogProperties.Facet facet = facet("eprel", 0, "never");

        final Set<ConstraintViolation<BillingCatalogProperties.Facet>> violations = validator.validate(facet);

        assertThat(violations).isEmpty();
    }

    @Test
    void rejectsEprelSourcedFacetWithNonZeroCredits() {
        final BillingCatalogProperties.Facet facet = facet("eprel", 5, "never");

        final Set<ConstraintViolation<BillingCatalogProperties.Facet>> violations = validator.validate(facet);

        assertThat(violations).isNotEmpty();
        assertThat(violations)
                .anyMatch(v -> v.getMessage().contains("eprel-sourced facet must declare credits: 0"));
    }

    @Test
    void rejectsEprelSourcedFacetThatCouldBecomeBillable() {
        final BillingCatalogProperties.Facet facet = facet("eprel", 0, "fresh-offer");

        final Set<ConstraintViolation<BillingCatalogProperties.Facet>> violations = validator.validate(facet);

        assertThat(violations).isNotEmpty();
    }

    @Test
    void nonEprelFacetsAreNotConstrainedBySourceRule() {
        final BillingCatalogProperties.Facet facet = facet(null, 5, "fresh-offer");

        final Set<ConstraintViolation<BillingCatalogProperties.Facet>> violations = validator.validate(facet);

        assertThat(violations).isEmpty();
    }

    private BillingCatalogProperties.Facet facet(final String source, final int credits, final String billableWhen) {
        final BillingCatalogProperties.Facet facet = new BillingCatalogProperties.Facet();
        facet.setPath("/api/v1/products/{gtin}/energy");
        facet.setDoc("products/energy");
        facet.setCredits(credits);
        facet.setBillableWhen(billableWhen);
        facet.setSource(source);
        return facet;
    }
}
