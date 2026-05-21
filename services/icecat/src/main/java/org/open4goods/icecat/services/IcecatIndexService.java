package org.open4goods.icecat.services;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.StreamSupport;

import org.open4goods.icecat.model.IcecatCategoryDocument;
import org.open4goods.icecat.model.IcecatFeatureDocument;
import org.open4goods.icecat.model.IcecatFeatureGroup;
import org.open4goods.icecat.model.IcecatFeatureGroupDocument;
import org.open4goods.icecat.model.IcecatName;
import org.open4goods.icecat.model.IcecatSupplierDocument;
import org.open4goods.icecat.repository.IcecatCategoryRepository;
import org.open4goods.icecat.repository.IcecatFeatureGroupRepository;
import org.open4goods.icecat.repository.IcecatFeatureRepository;
import org.open4goods.icecat.repository.IcecatSupplierRepository;
import org.open4goods.icecat.services.loader.CategoryLoader;
import org.open4goods.icecat.services.loader.FeatureLoader;
import org.open4goods.icecat.util.IcecatConstants;
import org.open4goods.model.helper.IdHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Manages Elasticsearch persistence for Icecat reference data.
 *
 * <p>After application startup the in-memory maps populated by {@link FeatureLoader} and
 * {@link CategoryLoader} are mirrored to dedicated ES indexes. This provides:
 * <ul>
 *   <li>Persistent reference data that survives application restarts without re-downloading XML.</li>
 *   <li>Searchable indexes for admin endpoints (category browse, feature lookup).</li>
 *   <li>Foundation for fuzzy vertical-to-category matching.</li>
 * </ul>
 *
 * <p>Attribute-name resolution is served from Elasticsearch through {@link IcecatFeatureResolver},
 * with a small runtime cache to avoid repeated identical queries.
 */
public class IcecatIndexService {

    private static final Logger LOGGER = LoggerFactory.getLogger(IcecatIndexService.class);

    private static final int LANG_ID_ENGLISH = IcecatConstants.LANG_ID_ENGLISH;

    private final FeatureLoader featureLoader;
    private final CategoryLoader categoryLoader;
    private final IcecatFeatureRepository featureRepository;
    private final IcecatCategoryRepository categoryRepository;
    private final IcecatFeatureGroupRepository featureGroupRepository;
    private final IcecatSupplierRepository supplierRepository;

    public IcecatIndexService(
            FeatureLoader featureLoader,
            CategoryLoader categoryLoader,
            IcecatFeatureRepository featureRepository,
            IcecatCategoryRepository categoryRepository,
            IcecatFeatureGroupRepository featureGroupRepository,
            IcecatSupplierRepository supplierRepository) {
        this.featureLoader = featureLoader;
        this.categoryLoader = categoryLoader;
        this.featureRepository = featureRepository;
        this.categoryRepository = categoryRepository;
        this.featureGroupRepository = featureGroupRepository;
        this.supplierRepository = supplierRepository;
    }

    /**
     * Triggered after all Spring beans are ready (after {@link FeatureLoader} and
     * {@link CategoryLoader} have loaded their in-memory maps).
     * Persists reference data to Elasticsearch indexes.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        syncFromLoaders();
    }

    /**
     * Synchronises all Icecat reference data from the in-memory loaders to Elasticsearch.
     * Safe to call multiple times (upserts by ID). Skips each index if the corresponding
     * loader map is empty (e.g. Icecat not configured).
     */
    public void syncFromLoaders() {
        LOGGER.info("Syncing Icecat reference data to Elasticsearch");
        try {
            syncFeatures();
            syncCategories();
            syncFeatureGroups();
            syncSuppliers();
            LOGGER.info("Icecat reference data sync complete");
        } catch (Exception e) {
            LOGGER.error("Error syncing Icecat reference data to Elasticsearch", e);
        }
    }

    private void syncFeatures() {
        if (featureLoader.getFeaturesById().isEmpty()) {
            LOGGER.info("Feature map is empty, skipping feature index sync");
            return;
        }
        List<IcecatFeatureDocument> docs = featureLoader.getFeaturesById().values().stream()
                .map(this::toFeatureDocument)
                .toList();
        featureRepository.saveAll(docs);
        LOGGER.info("Indexed {} Icecat features", docs.size());
    }

    private void syncCategories() {
        if (categoryLoader.getCategoriesById().isEmpty()) {
            LOGGER.info("Category map is empty, skipping category index sync");
            return;
        }
        List<IcecatCategoryDocument> docs = categoryLoader.getCategoriesById().values().stream()
                .map(cat -> {
                    IcecatCategoryDocument doc = new IcecatCategoryDocument();
                    doc.setId(cat.getId());
                    doc.setScore(cat.getScore());
                    if (cat.getParentCategory() != null) {
                        doc.setParentId(cat.getParentCategory().getId());
                    }
                    List<IcecatName> names = cat.getNames();
                    doc.setEnglishName(names.stream()
                            .filter(n -> n.getLangId() == LANG_ID_ENGLISH)
                            .map(IcecatName::getEffectiveName)
                            .findFirst()
                            .orElse(null));
                    doc.setLangNames(toLangNameList(names));
                    return doc;
                })
                .toList();
        categoryRepository.saveAll(docs);
        LOGGER.info("Indexed {} Icecat categories", docs.size());
    }

    private void syncFeatureGroups() {
        if (featureLoader.getFeatureGroupsById().isEmpty()) {
            LOGGER.info("Feature group map is empty, skipping feature group index sync");
            return;
        }
        List<IcecatFeatureGroupDocument> docs = featureLoader.getFeatureGroupsById().values().stream()
                .map(this::toFeatureGroupDocument)
                .toList();
        featureGroupRepository.saveAll(docs);
        LOGGER.info("Indexed {} Icecat feature groups", docs.size());
    }

    private void syncSuppliers() {
        if (featureLoader.getIcecatSuppliers().isEmpty()) {
            LOGGER.info("Supplier list is empty, skipping supplier index sync");
            return;
        }
        List<IcecatSupplierDocument> docs = featureLoader.getIcecatSuppliers().stream()
                .filter(s -> s.getId() != null)
                .map(supplier -> {
                    IcecatSupplierDocument doc = new IcecatSupplierDocument();
                    doc.setId(supplier.getId());
                    doc.setName(supplier.getEffectiveName());
                    doc.setLogoUrl(supplier.getBestLogoUrl());
                    doc.setLogoHighPic(supplier.getLogoHighPic());
                    doc.setLogoMediumPic(supplier.getLogoMediumPic());
                    doc.setLogoLowPic(supplier.getLogoLowPic());
                    doc.setLogoPic(supplier.getLogoPic());
                    return doc;
                })
                .toList();
        supplierRepository.saveAll(docs);
        LOGGER.info("Indexed {} Icecat suppliers", docs.size());
    }

    private IcecatFeatureDocument toFeatureDocument(org.open4goods.icecat.model.IcecatFeature feature) {
        IcecatFeatureDocument doc = new IcecatFeatureDocument();
        doc.setId(feature.getId());
        doc.setType(feature.getType());

        List<IcecatName> names = feature.getNames().getNames();

        doc.setEnglishName(names.stream()
                .filter(n -> n.getLangId() == LANG_ID_ENGLISH)
                .map(IcecatName::getEffectiveName)
                .findFirst()
                .orElse(null));

        Set<String> normalizedNames = new HashSet<>();
        names.forEach(n -> {
            String effective = n.getEffectiveName();
            if (effective != null) {
                normalizedNames.add(IdHelper.normalizeAttributeName(effective));
            }
        });
        doc.setNormalizedNames(normalizedNames);
        doc.setLangNames(toLangNameList(names));
        return doc;
    }

    private IcecatFeatureGroupDocument toFeatureGroupDocument(IcecatFeatureGroup fg) {
        IcecatFeatureGroupDocument doc = new IcecatFeatureGroupDocument();
        doc.setId(fg.getId());
        List<IcecatName> names = fg.getNames() != null ? fg.getNames() : List.of();
        doc.setEnglishName(names.stream()
                .filter(n -> n.getLangId() == LANG_ID_ENGLISH)
                .map(IcecatName::getEffectiveName)
                .findFirst()
                .orElse(null));
        doc.setLangNames(toLangNameList(names));
        return doc;
    }

    /** Encodes a list of Icecat names as {@code "langId:name"} strings for compact ES storage. */
    private List<String> toLangNameList(List<IcecatName> names) {
        List<String> result = new ArrayList<>();
        for (IcecatName n : names) {
            String effective = n.getEffectiveName();
            if (effective != null) {
                result.add(n.getLangId() + ":" + effective);
            }
        }
        return result;
    }

    // -------------------------------------------------------------------------
    // Query methods used by admin endpoints (Phase 4)
    // -------------------------------------------------------------------------

    /**
     * Searches Icecat features by English name.
     *
     * @param query    search terms
     * @param pageable pagination
     * @return matching feature documents
     */
    public Page<IcecatFeatureDocument> searchFeatures(String query, Pageable pageable) {
        return featureRepository.findByEnglishNameContaining(query, pageable);
    }

    /**
     * Searches Icecat categories by English name (used for fuzzy vertical matching).
     *
     * @param query    search terms
     * @param pageable pagination
     * @return matching category documents
     */
    public Page<IcecatCategoryDocument> searchCategories(String query, Pageable pageable) {
        return categoryRepository.findByEnglishNameContaining(query, pageable);
    }

    /**
     * Finds all Icecat categories (for admin browsing).
     *
     * @return all indexed categories
     */
    public Iterable<IcecatCategoryDocument> findAllCategories() {
        return categoryRepository.findAll();
    }

    /**
     * Finds a single Icecat category by ID.
     *
     * @param id category ID
     * @return category document, or empty if not found
     */
    public Optional<IcecatCategoryDocument> findCategory(Integer id) {
        return categoryRepository.findById(id);
    }

    /**
     * Finds a single Icecat feature by ID.
     *
     * @param id feature ID
     * @return feature document, or empty if not found
     */
    public Optional<IcecatFeatureDocument> findFeature(Integer id) {
        return featureRepository.findById(id);
    }

    /**
     * Finds features by normalised attribute name.
     *
     * @param normalizedName normalised name (see IdHelper.normalizeAttributeName)
     * @return matching feature documents
     */
    public List<IcecatFeatureDocument> findFeaturesByNormalizedName(String normalizedName) {
        return featureRepository.findByNormalizedName(normalizedName);
    }

    /**
     * Returns index counts, useful for health checks and admin dashboards.
     *
     * @return array of [featureCount, categoryCount, featureGroupCount, supplierCount]
     */
    public long[] indexCounts() {
        return new long[]{
                featureRepository.count(),
                categoryRepository.count(),
                featureGroupRepository.count(),
                supplierRepository.count()
        };
    }
}
