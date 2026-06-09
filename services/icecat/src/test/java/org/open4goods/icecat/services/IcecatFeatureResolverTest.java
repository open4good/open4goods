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

import org.open4goods.model.helper.IdHelper;

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

    @Test
    void warmUpPreventsEsLookupForKnownFeatureNames() {
        IcecatIndexService indexService = mock(IcecatIndexService.class);

        IcecatFeatureDocument doc = new IcecatFeatureDocument();
        doc.setId(99);
        doc.setEnglishName("Screen Size");
        String normalized = IdHelper.normalizeAttributeName("Screen Size");
        doc.setNormalizedNames(Set.of(normalized));

        IcecatFeatureResolver resolver = new IcecatFeatureResolver(indexService);
        resolver.warmUp(List.of(doc));

        assertThat(resolver.resolveFeatureName("Screen Size")).containsExactly(99);
        verify(indexService, never()).findFeaturesByNormalizedName(any());
    }
}
