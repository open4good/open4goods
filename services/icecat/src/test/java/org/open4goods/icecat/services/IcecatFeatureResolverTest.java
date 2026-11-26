package org.open4goods.icecat.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.open4goods.icecat.model.IcecatFeatureDocument;

class IcecatFeatureResolverTest {

    @Test
    void resolveFeatureNameCachesNormalizedLookups() {
        IcecatIndexService indexService = mock(IcecatIndexService.class);
        IcecatFeatureDocument document = new IcecatFeatureDocument();
        document.setId(46);

        when(indexService.findFeaturesByNormalizedName("COULEUR")).thenReturn(List.of(document));

        IcecatFeatureResolver resolver = new IcecatFeatureResolver(indexService);

        assertThat(resolver.resolveFeatureName("Couleur")).containsExactly(46);
        assertThat(resolver.resolveFeatureName("COULEUR")).containsExactly(46);
        verify(indexService, times(1)).findFeaturesByNormalizedName("COULEUR");
    }

    @Test
    void getFeatureNameUsesFeatureDocumentAlreadyLoadedDuringResolution() {
        IcecatIndexService indexService = mock(IcecatIndexService.class);
        IcecatFeatureDocument document = new IcecatFeatureDocument();
        document.setId(46);
        document.setEnglishName("Colour");
        document.setLangNames(List.of("1:Colour", "3:Couleur"));

        when(indexService.findFeaturesByNormalizedName("COULEUR")).thenReturn(List.of(document));

        IcecatFeatureResolver resolver = new IcecatFeatureResolver(indexService);

        assertThat(resolver.resolveFeatureName("Couleur")).isEqualTo(Set.of(46));
        assertThat(resolver.getFeatureName(46, "fr")).isEqualTo("Couleur");
        verify(indexService, times(0)).findFeature(46);
    }

    /**
     * GOU-2280: the Elasticsearch query of a cache miss runs outside the cache lock, so a slow lookup does not
     * hold back the completion threads that resolve other names.
     */
    @Test
    void slowLookupDoesNotBlockTheLookupOfAnotherName() throws Exception {
        IcecatIndexService indexService = mock(IcecatIndexService.class);
        CountDownLatch slowLookupStarted = new CountDownLatch(1);
        CountDownLatch otherLookupDone = new CountDownLatch(1);
        when(indexService.findFeaturesByNormalizedName("COULEUR")).thenAnswer(invocation -> {
            slowLookupStarted.countDown();
            otherLookupDone.await(10, TimeUnit.SECONDS);
            return List.of(feature(46));
        });
        when(indexService.findFeaturesByNormalizedName("POIDS")).thenReturn(List.of(feature(94)));
        IcecatFeatureResolver resolver = new IcecatFeatureResolver(indexService);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Set<Integer>> slow = executor.submit(() -> resolver.resolveFeatureName("Couleur"));
            assertThat(slowLookupStarted.await(5, TimeUnit.SECONDS)).isTrue();

            Future<Set<Integer>> other = executor.submit(() -> resolver.resolveFeatureName("Poids"));

            assertThat(other.get(5, TimeUnit.SECONDS)).containsExactly(94);
            otherLookupDone.countDown();
            assertThat(slow.get(5, TimeUnit.SECONDS)).containsExactly(46);
        } finally {
            otherLookupDone.countDown();
            executor.shutdownNow();
        }
    }

    private static IcecatFeatureDocument feature(int id) {
        IcecatFeatureDocument document = new IcecatFeatureDocument();
        document.setId(id);
        return document;
    }

    @Test
    void cacheSizesReflectPriorLookups() {
        IcecatIndexService indexService = mock(IcecatIndexService.class);
        IcecatFeatureDocument document = new IcecatFeatureDocument();
        document.setId(46);
        when(indexService.findFeaturesByNormalizedName("COULEUR")).thenReturn(List.of(document));

        IcecatFeatureResolver resolver = new IcecatFeatureResolver(indexService);

        assertThat(resolver.cacheSizes()).containsExactly(0, 0);

        resolver.resolveFeatureName("Couleur");

        int[] sizes = resolver.cacheSizes();
        assertThat(sizes[0]).isEqualTo(1);
        assertThat(sizes[1]).isEqualTo(1);
    }

    /** GOU-2306: a reimport of the Icecat features empties both caches, so the next lookups read the new index. */
    @Test
    void reimportEmptiesTheCaches() {
        IcecatIndexService indexService = mock(IcecatIndexService.class);
        IcecatFeatureDocument document = new IcecatFeatureDocument();
        document.setId(46);
        when(indexService.findFeaturesByNormalizedName("COULEUR")).thenReturn(List.of(document));
        org.mockito.ArgumentCaptor<Runnable> listener = org.mockito.ArgumentCaptor.forClass(Runnable.class);

        IcecatFeatureResolver resolver = new IcecatFeatureResolver(indexService);
        verify(indexService).addReimportListener(listener.capture());
        resolver.resolveFeatureName("Couleur");
        assertThat(resolver.cacheSizes()).containsExactly(1, 1);

        listener.getValue().run();

        assertThat(resolver.cacheSizes()).containsExactly(0, 0);
        resolver.resolveFeatureName("Couleur");
        verify(indexService, times(2)).findFeaturesByNormalizedName("COULEUR");
    }
}
