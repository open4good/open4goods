package org.open4goods.commons.services;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.open4goods.icecat.model.IcecatCategoryDocument;
import org.open4goods.icecat.model.IcecatFeature;
import org.open4goods.icecat.model.IcecatFeatureDocument;
import org.open4goods.icecat.model.IcecatName;
import org.open4goods.icecat.model.IcecatNames;
import org.open4goods.icecat.repository.IcecatCategoryRepository;
import org.open4goods.icecat.repository.IcecatFeatureGroupRepository;
import org.open4goods.icecat.repository.IcecatFeatureRepository;
import org.open4goods.icecat.repository.IcecatSupplierRepository;
import org.open4goods.icecat.services.IcecatIndexService;
import org.open4goods.icecat.services.loader.CategoryLoader;
import org.open4goods.icecat.services.loader.FeatureLoader;

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
                featureLoader, categoryLoader,
                featureRepository, categoryRepository,
                featureGroupRepository, supplierRepository);
    }

    @Test
    public void testSyncFromLoadersDoesNotThrowWhenEmpty() {
        assertDoesNotThrow(() -> indexService.syncFromLoaders());
        verify(featureRepository, never()).saveAll(anyList());
    }

    @Test
    public void testSyncIndexesFeatureWhenMapPopulated() {
        IcecatName nameEn = new IcecatName();
        nameEn.setLangId(1);
        nameEn.setValue("Screen size");

        IcecatNames names = new IcecatNames();
        names.setNames(Arrays.asList(nameEn));

        IcecatFeature feature = new IcecatFeature();
        feature.setId(42);
        feature.setType("numerical");
        feature.setNames(names);

        Map<Integer, IcecatFeature> map = new HashMap<>();
        map.put(42, feature);
        when(featureLoader.getFeaturesById()).thenReturn(map);

        when(featureRepository.saveAll(any())).thenReturn(Collections.emptyList());

        indexService.syncFromLoaders();

        verify(featureRepository, atLeastOnce()).saveAll(any());
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
}
