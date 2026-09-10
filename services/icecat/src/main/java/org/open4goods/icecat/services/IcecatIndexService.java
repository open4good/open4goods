package org.open4goods.icecat.services;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.StreamSupport;

import org.open4goods.icecat.jaxb.Feature;
import org.open4goods.icecat.jaxb.FeatureGroup;
import org.open4goods.icecat.jaxb.Name;
import org.open4goods.icecat.model.IcecatCategoryFeatureDocument;
import org.open4goods.icecat.model.IcecatCategoryFeatureGroupDocument;
import org.open4goods.icecat.model.IcecatCategoryDocument;
import org.open4goods.icecat.model.IcecatFeatureDocument;
import org.open4goods.icecat.model.IcecatFeatureGroupDocument;
import org.open4goods.icecat.model.IcecatSupplierDocument;
import org.open4goods.icecat.repository.IcecatCategoryRepository;
import org.open4goods.icecat.repository.IcecatFeatureGroupRepository;
import org.open4goods.icecat.repository.IcecatFeatureRepository;
import org.open4goods.icecat.repository.IcecatSupplierRepository;
import org.open4goods.icecat.services.loader.CategoryLoader;
import org.open4goods.icecat.services.loader.FeatureLoader;
import org.open4goods.icecat.services.loader.IcecatBulkModelSupport;
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
    private IcecatFeatureResolver featureResolver;

    private final Map<String, List<IcecatFeatureDocument>> featureCache = java.util.Collections.synchronizedMap(
        new java.util.LinkedHashMap<String, List<IcecatFeatureDocument>>(256, 0.75f, true) {
            private static final long serialVersionUID = 1L;
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, List<IcecatFeatureDocument>> eldest) {
                return size() > 10000;
            }
        }
    );

    public void setFeatureResolver(IcecatFeatureResolver featureResolver) {
        this.featureResolver = featureResolver;
    }

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
        featureCache.clear();
        if (featureResolver != null) {
            featureResolver.warmUp(docs);
        }
    }

    private void syncCategories() {
        if (categoryLoader.getCategoriesById().isEmpty()) {
            LOGGER.info("Category map is empty, skipping category index sync");
            return;
        }
        List<IcecatCategoryDocument> docs = categoryLoader.getCategoriesById().values().stream()
                .map(cat -> {
                    IcecatCategoryDocument doc = new IcecatCategoryDocument();
                    doc.setId(IcecatBulkModelSupport.intValue(cat.getID()));
                    doc.setScore(IcecatBulkModelSupport.intValue(cat.getScore()));
                    if (cat.getParentCategory() != null) {
                        doc.setParentId(IcecatBulkModelSupport.intValue(cat.getParentCategory().getID()));
                    }
                    List<Name> names = cat.getName();
                    doc.setEnglishName(names.stream()
                            .filter(n -> IcecatBulkModelSupport.intValue(n.getLangid(), -1) == LANG_ID_ENGLISH)
                            .map(IcecatBulkModelSupport::effectiveName)
                            .findFirst()
                            .orElse(null));
                    doc.setLangNames(toLangNameList(names));
                    doc.setFeatureGroups(cat.getCategoryFeatureGroup().stream()
                            .map(cfg -> {
                                IcecatCategoryFeatureGroupDocument group = new IcecatCategoryFeatureGroupDocument();
                                group.setId(IcecatBulkModelSupport.intValue(cfg.getID()));
                                group.setFeatureGroupIds(cfg.getFeatureGroup().stream()
                                        .map(fg -> IcecatBulkModelSupport.intValue(fg.getID()))
                                        .toList());
                                return group;
                            })
                            .toList());
                    doc.setFeatures(cat.getFeature().stream()
                            .map(this::toCategoryFeatureDocument)
                            .toList());
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
                .filter(s -> s.getID() != null)
                .map(supplier -> {
                    IcecatSupplierDocument doc = new IcecatSupplierDocument();
                    doc.setId(IcecatBulkModelSupport.intValue(supplier.getID()));
                    doc.setName(IcecatBulkModelSupport.effectiveName(supplier));
                    doc.setLogoUrl(IcecatBulkModelSupport.bestLogoUrl(supplier));
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

    private IcecatCategoryFeatureDocument toCategoryFeatureDocument(Feature feature) {
        IcecatCategoryFeatureDocument doc = new IcecatCategoryFeatureDocument();
        doc.setId(IcecatBulkModelSupport.intValue(feature.getID()));
        doc.setType(feature.getType());
        doc.setCategoryFeatureGroupId(IcecatBulkModelSupport.intValue(feature.getCategoryFeatureGroupID(), 0));
        doc.setCategoryFeatureId(IcecatBulkModelSupport.intValue(feature.getCategoryFeatureID(), 0));
        doc.setLimitDirection(IcecatBulkModelSupport.intValue(feature.getLimitDirection(), 0));
        doc.setMandatory(IcecatBulkModelSupport.intValue(feature.getMandatory(), 0));
        doc.setSearchable((feature.isSetSearchable() && feature.isSearchable()) ? 1 : 0);
        doc.setNo(feature.getNo() != null ? feature.getNo().toString() : null);
        doc.setClazz(feature.isSetClazz() ? String.valueOf(feature.isClazz()) : null);
        doc.setDefaultDisplayUnit(feature.isSetDefaultDisplayUnit() ? String.valueOf(feature.isDefaultDisplayUnit()) : null);
        doc.setUseDropdownInput(feature.getUseDropdownInput());
        doc.setValueSorting(IcecatBulkModelSupport.intValue(feature.getValueSorting(), 0));
        return doc;
    }

    private IcecatFeatureDocument toFeatureDocument(Feature feature) {
        IcecatFeatureDocument doc = new IcecatFeatureDocument();
        doc.setId(IcecatBulkModelSupport.intValue(feature.getID()));
        doc.setType(feature.getType());

        List<Name> names = feature.getNames().getName();

        doc.setEnglishName(names.stream()
                .filter(n -> IcecatBulkModelSupport.intValue(n.getLangid(), -1) == LANG_ID_ENGLISH)
                .map(IcecatBulkModelSupport::effectiveName)
                .findFirst()
                .orElse(null));

        Set<String> normalizedNames = new HashSet<>();
        names.forEach(n -> {
            String effective = IcecatBulkModelSupport.effectiveName(n);
            if (effective != null) {
                normalizedNames.add(IdHelper.normalizeAttributeName(effective));
            }
        });
        doc.setNormalizedNames(normalizedNames);
        doc.setLangNames(toLangNameList(names));
        return doc;
    }

    private IcecatFeatureGroupDocument toFeatureGroupDocument(FeatureGroup fg) {
        IcecatFeatureGroupDocument doc = new IcecatFeatureGroupDocument();
        doc.setId(IcecatBulkModelSupport.intValue(fg.getID()));
        List<Name> names = fg.getName();
        doc.setEnglishName(names.stream()
                .filter(n -> IcecatBulkModelSupport.intValue(n.getLangid(), -1) == LANG_ID_ENGLISH)
                .map(IcecatBulkModelSupport::effectiveName)
                .findFirst()
                .orElse(null));
        doc.setLangNames(toLangNameList(names));
        return doc;
    }

    /** Encodes a list of Icecat names as {@code "langId:name"} strings for compact ES storage. */
    private List<String> toLangNameList(List<Name> names) {
        List<String> result = new ArrayList<>();
        for (Name n : names) {
            String effective = IcecatBulkModelSupport.effectiveName(n);
            if (effective != null) {
                result.add(IcecatBulkModelSupport.intValue(n.getLangid(), 0) + ":" + effective);
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
     * Returns all feature documents referenced by an indexed category, preserving the
     * order declared by Icecat in the category-feature export.
     *
     * @param category the category document carrying category-feature metadata
     * @return feature documents keyed by their Icecat feature ID
     */
    public Map<Integer, IcecatFeatureDocument> findCategoryFeatureDocuments(IcecatCategoryDocument category) {
        List<IcecatCategoryFeatureDocument> categoryFeatures = category.getFeatures() == null
                ? List.of()
                : category.getFeatures();
        List<Integer> ids = categoryFeatures.stream()
                .map(IcecatCategoryFeatureDocument::getId)
                .filter(id -> id != null)
                .distinct()
                .toList();
        Map<Integer, IcecatFeatureDocument> result = new LinkedHashMap<>();
        StreamSupport.stream(featureRepository.findAllById(ids).spliterator(), false)
                .sorted(Comparator.comparingInt(doc -> ids.indexOf(doc.getId())))
                .forEach(doc -> result.put(doc.getId(), doc));
        return result;
    }

    /**
     * Finds features by normalised attribute name.
     *
     * @param normalizedName normalised name (see IdHelper.normalizeAttributeName)
     * @return matching feature documents
     */
    public List<IcecatFeatureDocument> findFeaturesByNormalizedName(String normalizedName) {
        if (normalizedName == null) {
            return List.of();
        }
        return featureCache.computeIfAbsent(normalizedName, featureRepository::findByNormalizedName);
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
