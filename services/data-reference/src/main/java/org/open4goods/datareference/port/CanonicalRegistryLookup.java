package org.open4goods.datareference.port;

import java.util.Optional;
import java.time.LocalDate;

import org.open4goods.datareference.model.CanonicalAttributeId;
import org.open4goods.datareference.model.CanonicalClassId;
import org.open4goods.datareference.model.registry.CanonicalAttributeDefinition;
import org.open4goods.datareference.model.registry.CanonicalClassDefinition;
import org.open4goods.datareference.model.registry.RegistryExternalMapping;
import org.open4goods.datareference.model.registry.RegistryVerticalView;
import org.open4goods.datareference.model.registry.RegistryVersion;

/**
 * Read access to the authored O4G registry.
 *
 * <p>Read-only by design: the registry is authored in Git and projected to a
 * runtime index, so a runtime write that could redefine a concept would make
 * the Git copy stop being the authority.
 */
public interface CanonicalRegistryLookup {

    /**
     * Returns the registry version these lookups answer for.
     *
     * @return current registry version
     */
    RegistryVersion version();

    /**
     * Looks up one canonical attribute.
     *
     * @param id canonical attribute identifier
     * @return its definition, or empty when the registry does not declare it
     */
    Optional<CanonicalAttributeDefinition> findAttribute(CanonicalAttributeId id);

    /**
     * Looks up one canonical class.
     *
     * @param id canonical class identifier
     * @return its definition, or empty when the registry does not declare it
     */
    Optional<CanonicalClassDefinition> findClass(CanonicalClassId id);

    /**
     * Resolves one reviewed provider coordinate at a specified effective date.
     *
     * <p>Proposed and rejected mappings are deliberately not visible through
     * this read port. Provider taxonomies can therefore be recorded for review
     * without becoming an authority that creates O4G concepts at runtime.
     *
     * @param system provider-neutral mapping system
     * @param externalId opaque provider coordinate
     * @param effectiveOn date for which the mapping must be effective
     * @return reviewed canonical mapping, or empty when none is effective
     */
    Optional<RegistryExternalMapping> findReviewedMapping(String system, String externalId, LocalDate effectiveOn);

    /**
     * Returns an editorial vertical view without treating its id as a class id.
     *
     * @param verticalId stable editorial vertical identifier
     * @return explicitly included O4G classes, or empty when the view is unknown
     */
    Optional<RegistryVerticalView> findVerticalView(String verticalId);
}
