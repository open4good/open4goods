package org.open4goods.commons.services;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.math.BigInteger;
import java.util.Collections;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.open4goods.icecat.jaxb.Category;
import org.open4goods.icecat.jaxb.CategoryFeatureGroup;
import org.open4goods.icecat.jaxb.Feature;
import org.open4goods.icecat.jaxb.FeatureGroup;
import org.open4goods.icecat.jaxb.Name;
import org.open4goods.icecat.jaxb.Names;
import org.open4goods.icecat.model.IcecatCategoryDocument;
import org.open4goods.icecat.model.IcecatCategoryFeatureDocument;
import org.open4goods.icecat.model.IcecatFeatureDocument;
import org.open4goods.icecat.config.yml.IcecatConfiguration;
import org.open4goods.icecat.repository.IcecatCategoryRepository;
import org.open4goods.icecat.repository.IcecatFeatureGroupRepository;
import org.open4goods.icecat.repository.IcecatFeatureRepository;
import org.open4goods.icecat.repository.IcecatSupplierRepository;
import org.open4goods.icecat.services.IcecatIndexService;
import org.open4goods.icecat.services.loader.CategoryLoader;
import org.open4goods.icecat.services.loader.FeatureLoader;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;

/**
 * Unit-level coverage for {@link IcecatIndexService}: JAXB-to-document mapping (via reflection,
 * since mapping is private) and the plain read/delegate methods. The versioned-index import
 * pipeline itself ({@code syncFromLoaders()} end to end, alias switching, rollback) is covered
 * against a real Elasticsearch by {@code IcecatIndexVersionManagerIT}, not here — mocking
 * {@link ElasticsearchOperations}'s full index/alias API would only verify the mock, not the
 * pipeline.
 */
public class IcecatIndexServiceTest {

    private FeatureLoader featureLoader;
    private CategoryLoader categoryLoader;
    private IcecatFeatureRepository featureRepository;
    private IcecatCategoryRepository categoryRepository;
    private IcecatFeatureGroupRepository featureGroupRepository;
    private IcecatSupplierRepository supplierRepository;
    private IcecatIndexService indexService;

    @BeforeEach
    public void setUp() {
        featureLoader = Mockito.mock(FeatureLoader.class);
        categoryLoader = Mockito.mock(CategoryLoader.class);
        featureRepository = Mockito.mock(IcecatFeatureRepository.class);
        categoryRepository = Mockito.mock(IcecatCategoryRepository.class);
        featureGroupRepository = Mockito.mock(IcecatFeatureGroupRepository.class);
        supplierRepository = Mockito.mock(IcecatSupplierRepository.class);

        when(featureLoader.getFeaturesById()).thenReturn(Collections.emptyMap());
        when(featureLoader.getFeatureGroupsById()).thenReturn(Collections.emptyMap());
        when(featureLoader.getIcecatSuppliers()).thenReturn(Collections.emptyList());
        when(categoryLoader.getCategoriesById()).thenReturn(Collections.emptyMap());

        indexService = new IcecatIndexService(
                Mockito.mock(IcecatConfiguration.class),
                featureLoader, categoryLoader,
                featureRepository, categoryRepository,
                featureGroupRepository, supplierRepository,
                Mockito.mock(ElasticsearchOperations.class));
    }

    @Test
    public void testSyncFromLoadersDoesNotThrowWhenEmpty() {
        assertDoesNotThrow(() -> indexService.syncFromLoaders());
    }

    @Test
    public void testToFeatureDocumentMapsNameAndType() throws Exception {
        Name nameEn = new Name();
        nameEn.setLangid(BigInteger.valueOf(1));
        nameEn.setValueAttribute("Screen size");

        Names names = new Names();
        names.getName().add(nameEn);

        Feature feature = new Feature();
        feature.setID(BigInteger.valueOf(42));
        feature.setType("numerical");
        feature.setNames(names);

        Method toFeatureDocument = IcecatIndexService.class.getDeclaredMethod("toFeatureDocument", Feature.class);
        toFeatureDocument.setAccessible(true);
        IcecatFeatureDocument doc = (IcecatFeatureDocument) toFeatureDocument.invoke(indexService, feature);

        assertEquals(42, doc.getId());
        assertEquals("numerical", doc.getType());
        assertEquals("Screen size", doc.getEnglishName());
        assertEquals("1:Screen size", doc.getLangNames().get(0));
    }

    @Test
    public void testToCategoryDocumentMapsFeatureGroupsAndFeatures() throws Exception {
        Name nameEn = new Name();
        nameEn.setLangid(BigInteger.valueOf(1));
        nameEn.setValueAttribute("Washing Machines");

        FeatureGroup featureGroup = new FeatureGroup();
        featureGroup.setID(BigInteger.valueOf(88));

        CategoryFeatureGroup categoryFeatureGroup = new CategoryFeatureGroup();
        categoryFeatureGroup.setID(BigInteger.valueOf(77));
        categoryFeatureGroup.getFeatureGroup().add(featureGroup);

        Feature feature = new Feature();
        feature.setID(BigInteger.valueOf(42));
        feature.setCategoryFeatureGroupID(BigInteger.valueOf(77));
        feature.setCategoryFeatureID(BigInteger.valueOf(9001));
        feature.setMandatory(BigInteger.ONE);
        feature.setSearchable(true);
        feature.setDefaultDisplayUnit(true);

        Category category = new Category();
        category.setID(BigInteger.valueOf(123));
        category.getName().add(nameEn);
        category.getCategoryFeatureGroup().add(categoryFeatureGroup);
        category.getFeature().add(feature);

        Method toCategoryDocument = IcecatIndexService.class.getDeclaredMethod("toCategoryDocument", Category.class);
        toCategoryDocument.setAccessible(true);
        IcecatCategoryDocument document = (IcecatCategoryDocument) toCategoryDocument.invoke(indexService, category);

        assertEquals(123, document.getId());
        assertEquals("Washing Machines", document.getEnglishName());
        assertEquals(77, document.getFeatureGroups().get(0).getId());
        assertEquals(88, document.getFeatureGroups().get(0).getFeatureGroupIds().get(0));
        IcecatCategoryFeatureDocument featureDocument = document.getFeatures().get(0);
        assertEquals(42, featureDocument.getId());
        assertEquals(77, featureDocument.getCategoryFeatureGroupId());
        assertEquals(9001, featureDocument.getCategoryFeatureId());
        assertEquals(1, featureDocument.getMandatory());
        assertEquals("true", featureDocument.getDefaultDisplayUnit());
    }

    @Test
    public void testFindFeatureDelegatesToRepository() {
        IcecatFeatureDocument doc = new IcecatFeatureDocument();
        doc.setId(7);
        doc.setEnglishName("Weight");
        when(featureRepository.findById(7)).thenReturn(Optional.of(doc));

        Optional<IcecatFeatureDocument> result = indexService.findFeature(7);

        assertNotNull(result);
        assertEquals(true, result.isPresent());
        assertEquals("Weight", result.get().getEnglishName());
    }

    @Test
    public void testIndexCountsReturnRepositoryCounts() {
        when(featureRepository.count()).thenReturn(1000L);
        when(categoryRepository.count()).thenReturn(500L);
        when(featureGroupRepository.count()).thenReturn(200L);
        when(supplierRepository.count()).thenReturn(50L);

        long[] counts = indexService.indexCounts();

        assertEquals(1000L, counts[0]);
        assertEquals(500L, counts[1]);
        assertEquals(200L, counts[2]);
        assertEquals(50L, counts[3]);
    }

    @Test
    public void testFeatureCacheSizeStartsEmpty() {
        assertEquals(0, indexService.featureCacheSize());
    }
}
