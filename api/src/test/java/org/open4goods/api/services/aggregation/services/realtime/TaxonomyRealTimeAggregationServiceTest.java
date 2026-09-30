package org.open4goods.api.services.aggregation.services.realtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.open4goods.icecat.services.IcecatCategoryVerticalResolver;
import org.open4goods.model.product.Product;
import org.open4goods.model.product.ProductTexts;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.verticals.VerticalsConfigService;
import org.slf4j.LoggerFactory;

/**
 * Unit tests for {@link TaxonomyRealTimeAggregationService}, focused on the
 * regression where correctly-categorized products lost their vertical (causing
 * canonical-URL changes and SEO redirects), and on the reviewed-Icecat-registry
 * classification path.
 */
class TaxonomyRealTimeAggregationServiceTest {

	private VerticalsConfigService verticalService;
	private IcecatCategoryVerticalResolver icecatCategoryVerticalResolver;
	private TaxonomyRealTimeAggregationService service;

	@BeforeEach
	void setUp() {
		verticalService = mock(VerticalsConfigService.class);
		icecatCategoryVerticalResolver = mock(IcecatCategoryVerticalResolver.class);
		when(icecatCategoryVerticalResolver.resolveVerticalId(any(), any())).thenReturn(Optional.empty());
		service = new TaxonomyRealTimeAggregationService(
				LoggerFactory.getLogger(TaxonomyRealTimeAggregationServiceTest.class), verticalService,
				icecatCategoryVerticalResolver);
	}

	private VerticalConfig vertical(final String id, final Integer taxonomyId) {
		final VerticalConfig vc = new VerticalConfig();
		vc.setId(id);
		vc.setGoogleTaxonomyId(taxonomyId);
		return vc;
	}

	@Test
	void assignsVerticalAndTaxonomyFromCategories() throws Exception {
		final Product p = new Product();
		p.getCategoriesByDatasources().put("fnac.com", "TV & Home cinema");
		when(verticalService.getVerticalForCategories(anyMap())).thenReturn(vertical("tv", 404));

		service.onProduct(p, mock(VerticalConfig.class));

		assertEquals("tv", p.getVertical());
		assertEquals(Integer.valueOf(404), p.getGoogleTaxonomyId());
	}

	@Test
	void keepsVerticalEvenWhenOfferNamesLackCategoryToken() throws Exception {
		// Regression: the removed name-token "confirmation" step used to strip the
		// vertical because the merchant title contained no French taxonomy word.
		final Product p = new Product();
		p.setVertical("tv");
		p.getOfferNames().add("Samsung QE55 neo qled");
		p.getCategoriesByDatasources().put("fnac.com", "TV & Home cinema");
		when(verticalService.getVerticalForCategories(anyMap())).thenReturn(vertical("tv", 404));

		service.onProduct(p, mock(VerticalConfig.class));

		assertEquals("tv", p.getVertical());
	}

	@Test
	void clearsVerticalAndTaxonomyButKeepsNamesWhenNoCategoryMatches() throws Exception {
		final Product p = new Product();
		p.setVertical("tv");
		p.setGoogleTaxonomyId(404);
		final ProductTexts names = new ProductTexts();
		p.setNames(names);
		when(verticalService.getVerticalForCategories(anyMap())).thenReturn(null);

		service.onProduct(p, mock(VerticalConfig.class));

		assertNull(p.getVertical());
		assertNull(p.getGoogleTaxonomyId());
		// Names are not wiped here; slug (re)generation is delegated downstream to
		// NamesAggregationService.
		assertSame(names, p.getNames());
	}

	@Test
	void migratesToNewVerticalWithoutRevertingToOld() throws Exception {
		final Product p = new Product();
		p.setVertical("washing-machine");
		p.getOfferNames().add("some product");
		p.getCategoriesByDatasources().put("fnac.com", "TV & Home cinema");
		when(verticalService.getVerticalForCategories(anyMap())).thenReturn(vertical("tv", 404));

		service.onProduct(p, mock(VerticalConfig.class));

		assertEquals("tv", p.getVertical());
	}

	@Test
	void assignsVerticalFromApprovedIcecatMappingWhenCategoryIdIsPresent() throws Exception {
		final Product p = new Product();
		p.setIcecatCategoryId(1649);
		when(icecatCategoryVerticalResolver.resolveVerticalId(any(), any())).thenReturn(Optional.of("tv"));
		when(verticalService.getConfigById("tv")).thenReturn(vertical("tv", 404));

		service.onProduct(p, mock(VerticalConfig.class));

		assertEquals("tv", p.getVertical());
		assertEquals(Integer.valueOf(404), p.getGoogleTaxonomyId());
		verify(verticalService, never()).getVerticalForCategories(anyMap());
	}

	@Test
	void clearsVerticalWhenIcecatCategoryHasNoApprovedMapping() throws Exception {
		// Product decision: no approved mapping means no vertical. The legacy
		// category-name heuristic must not substitute an unreviewed guess.
		final Product p = new Product();
		p.setVertical("tv");
		p.setGoogleTaxonomyId(404);
		p.setIcecatCategoryId(999);
		p.getCategoriesByDatasources().put("fnac.com", "TV & Home cinema");
		when(icecatCategoryVerticalResolver.resolveVerticalId(any(), any())).thenReturn(Optional.empty());

		service.onProduct(p, mock(VerticalConfig.class));

		assertNull(p.getVertical());
		assertNull(p.getGoogleTaxonomyId());
		verify(verticalService, never()).getVerticalForCategories(anyMap());
	}
}
