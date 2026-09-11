package org.open4goods.datareference.model.registry;

import java.util.Collections;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.open4goods.datareference.model.CanonicalAttributeId;
import org.open4goods.datareference.model.CanonicalClassId;
import org.open4goods.datareference.model.CanonicalConceptId;
import org.open4goods.datareference.port.CanonicalRegistryLookup;

/**
 * Strict, immutable runtime representation of one Git-authored registry version.
 *
 * <p>This class deliberately implements only the read port. The importer that
 * later writes this snapshot to an aliased store may replace an entire version,
 * but no runtime caller can add or alter a definition.
 */
public final class InMemoryCanonicalRegistry implements CanonicalRegistryLookup {

    private final RegistryVersion version;
    private final Map<CanonicalAttributeId, CanonicalAttributeDefinition> attributes;
    private final Map<CanonicalClassId, CanonicalClassDefinition> classes;
    private final Map<ExternalMappingCoordinate, List<RegistryExternalMapping>> reviewedMappings;
    private final Map<String, RegistryVerticalView> verticalViews;

    /**
     * Validates all cross-definition references and builds immutable lookup maps.
     *
     * @param document complete authored registry document
     */
    public InMemoryCanonicalRegistry(RegistryDocument document) {
        Objects.requireNonNull(document, "document must not be null");
        version = document.registryVersion();
        attributes = byAttributeId(document);
        classes = byClassId(document);
        validateClassReferences();
        validateMappingIntervals(document);
        reviewedMappings = indexReviewedMappings();
        verticalViews = indexVerticalViews(document);
    }

    @Override
    public RegistryVersion version() {
        return version;
    }

    @Override
    public Optional<CanonicalAttributeDefinition> findAttribute(CanonicalAttributeId id) {
        return Optional.ofNullable(attributes.get(Objects.requireNonNull(id, "id must not be null")));
    }

    @Override
    public Optional<CanonicalClassDefinition> findClass(CanonicalClassId id) {
        return Optional.ofNullable(classes.get(Objects.requireNonNull(id, "id must not be null")));
    }

    @Override
    public Optional<RegistryExternalMapping> findReviewedMapping(String system, String externalId, LocalDate effectiveOn) {
        ExternalMappingCoordinate coordinate = new ExternalMappingCoordinate(system, externalId);
        Objects.requireNonNull(effectiveOn, "effectiveOn must not be null");
        return reviewedMappings.getOrDefault(coordinate, List.of()).stream()
                .filter(candidate -> candidate.mapping().isEffectiveOn(effectiveOn))
                .findFirst();
    }

    @Override
    public Optional<RegistryVerticalView> findVerticalView(String verticalId) {
        return Optional.ofNullable(verticalViews.get(Objects.requireNonNull(verticalId, "verticalId must not be null")));
    }

    /**
     * Returns the registry's declared attribute count for import reconciliation.
     *
     * @return number of addressable attributes
     */
    public int attributeCount() {
        return attributes.size();
    }

    /**
     * Returns the registry's declared class count for import reconciliation.
     *
     * @return number of addressable classes
     */
    public int classCount() {
        return classes.size();
    }

    private static Map<CanonicalAttributeId, CanonicalAttributeDefinition> byAttributeId(RegistryDocument document) {
        Map<CanonicalAttributeId, CanonicalAttributeDefinition> indexed = new LinkedHashMap<>();
        for (CanonicalAttributeDefinition definition : document.attributes()) {
            if (indexed.put(definition.id(), definition) != null) {
                throw new RegistryValidationException("duplicate attribute id: " + definition.id());
            }
        }
        return Collections.unmodifiableMap(indexed);
    }

    private static Map<CanonicalClassId, CanonicalClassDefinition> byClassId(RegistryDocument document) {
        Map<CanonicalClassId, CanonicalClassDefinition> indexed = new LinkedHashMap<>();
        for (CanonicalClassDefinition definition : document.classes()) {
            if (indexed.put(definition.id(), definition) != null) {
                throw new RegistryValidationException("duplicate class id: " + definition.id());
            }
        }
        return Collections.unmodifiableMap(indexed);
    }

    private void validateClassReferences() {
        for (CanonicalClassDefinition definition : classes.values()) {
            if (definition.parent() != null && !classes.containsKey(definition.parent())) {
                throw new RegistryValidationException("class " + definition.id() + " has an unknown parent: "
                        + definition.parent());
            }
            definition.attributes().forEach(attribute -> {
                if (!attributes.containsKey(attribute)) {
                    throw new RegistryValidationException("class " + definition.id()
                            + " references an unknown attribute: " + attribute);
                }
            });
            requireAcyclic(definition.id(), new HashSet<>());
        }
    }

    private void requireAcyclic(CanonicalClassId classId, Set<CanonicalClassId> path) {
        if (!path.add(classId)) {
            throw new RegistryValidationException("class hierarchy has a cycle at " + classId);
        }
        CanonicalClassId parent = classes.get(classId).parent();
        if (parent != null) {
            requireAcyclic(parent, path);
        }
        path.remove(classId);
    }

    private static void validateMappingIntervals(RegistryDocument document) {
        Map<String, List<ExternalMapping>> reviewedCoordinates = new LinkedHashMap<>();
        document.attributes().forEach(definition -> validateMappings(definition.id().toString(), definition.mappings(),
                reviewedCoordinates));
        document.classes().forEach(definition -> validateMappings(definition.id().toString(), definition.mappings(),
                reviewedCoordinates));
    }

    private Map<ExternalMappingCoordinate, List<RegistryExternalMapping>> indexReviewedMappings() {
        Map<ExternalMappingCoordinate, List<RegistryExternalMapping>> indexed = new LinkedHashMap<>();
        attributes.forEach((id, definition) -> indexMappings(indexed, id, definition.mappings()));
        classes.forEach((id, definition) -> indexMappings(indexed, id, definition.mappings()));
        indexed.replaceAll((coordinate, mappings) -> List.copyOf(mappings));
        return Collections.unmodifiableMap(indexed);
    }

    private static void indexMappings(Map<ExternalMappingCoordinate, List<RegistryExternalMapping>> indexed,
            CanonicalConceptId conceptId, List<ExternalMapping> mappings) {
        mappings.stream()
                .filter(mapping -> mapping.status() == ExternalMappingStatus.REVIEWED)
                .forEach(mapping -> indexed.computeIfAbsent(
                        new ExternalMappingCoordinate(mapping.system(), mapping.externalId()), ignored -> new java.util.ArrayList<>())
                        .add(new RegistryExternalMapping(conceptId, mapping)));
    }

    private static void validateMappings(String conceptId, Iterable<ExternalMapping> mappings,
            Map<String, List<ExternalMapping>> reviewedCoordinates) {
        for (ExternalMapping mapping : mappings) {
            if (mapping.status() != ExternalMappingStatus.REVIEWED) {
                continue;
            }
            String coordinate = mapping.system() + "\u0000" + mapping.externalId();
            List<ExternalMapping> existing = reviewedCoordinates.getOrDefault(coordinate, List.of());
            if (existing.stream().anyMatch(known -> known.overlaps(mapping))) {
                throw new RegistryValidationException("overlapping reviewed mapping for " + coordinate
                        + " includes " + conceptId);
            }
            reviewedCoordinates.computeIfAbsent(coordinate, ignored -> new java.util.ArrayList<>()).add(mapping);
        }
    }

    private Map<String, RegistryVerticalView> indexVerticalViews(RegistryDocument document) {
        Map<String, RegistryVerticalView> indexed = new LinkedHashMap<>();
        for (RegistryVerticalView view : document.verticalViews()) {
            if (indexed.put(view.verticalId(), view) != null) {
                throw new RegistryValidationException("duplicate vertical view id: " + view.verticalId());
            }
            for (CanonicalClassId classId : view.includedClasses()) {
                if (!classes.containsKey(classId)) {
                    throw new RegistryValidationException("vertical view " + view.verticalId()
                            + " references an unknown class: " + classId);
                }
            }
        }
        return Collections.unmodifiableMap(indexed);
    }
}
