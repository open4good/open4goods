package org.open4goods.datareference.port;

import java.util.Optional;

import org.open4goods.datareference.model.CanonicalAttributeId;
import org.open4goods.datareference.model.CanonicalClassId;
import org.open4goods.datareference.model.registry.CanonicalAttributeDefinition;
import org.open4goods.datareference.model.registry.CanonicalClassDefinition;
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
}
