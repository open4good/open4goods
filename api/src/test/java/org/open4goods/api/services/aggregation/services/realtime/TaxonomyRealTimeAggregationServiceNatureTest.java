package org.open4goods.api.services.aggregation.services.realtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.open4goods.model.eprel.EprelProduct;
import org.open4goods.model.helper.IdHelper;
import org.open4goods.model.product.Product;
import org.open4goods.model.product.ProductNature;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.services.serialisation.service.SerialisationService;
import org.open4goods.verticals.GoogleTaxonomyService;
import org.open4goods.verticals.VerticalsConfigService;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

/**
 * Tests the {@code nature} set by {@link TaxonomyRealTimeAggregationService#onProduct}
 * against the real vertical configurations and the markers of {@code _default.yml}
 * (GOU-2233, measure 4e.0 of GOU-2226).
 */
class TaxonomyRealTimeAggregationServiceNatureTest {

	private static VerticalsConfigService verticalService;
	private static TaxonomyRealTimeAggregationService service;

	@BeforeAll
	static void setUp() {
		verticalService = new VerticalsConfigService(new SerialisationService(), mock(GoogleTaxonomyService.class),
				new PathMatchingResourcePatternResolver());
		service = new TaxonomyRealTimeAggregationService(
				LoggerFactory.getLogger(TaxonomyRealTimeAggregationServiceNatureTest.class), verticalService);
	}

	/** Builds a product from raw feed categories, normalised as {@code DataFragment} does. */
	private static Product product(final String... datasourceAndCategory) {
		final Product p = new Product();
		for (int i = 0; i < datasourceAndCategory.length; i += 2) {
			p.getCategoriesByDatasources().put(datasourceAndCategory[i],
					IdHelper.getCategoryName(datasourceAndCategory[i + 1]));
		}
		return p;
	}

	private static ProductNature natureOf(final Product p) throws Exception {
		service.onProduct(p, mock(VerticalConfig.class));
		return p.getNature();
	}

	@Test
	void dartySparePartCategoryGivesSparePart() throws Exception {
		final Product p = product("darty.com", "PIECES DETACHEES LAVE-LINGE > BAC PRODUIT LAVE LINGE");

		assertThat(natureOf(p)).isEqualTo(ProductNature.SPARE_PART);
		assertThat(p.getVertical()).isNull();
	}

	@Test
	void cdiscountCleaningProductGivesAccessory() throws Exception {
		assertThat(natureOf(product("cdiscount.com", "DROGUERIE > PRODUIT D'ENTRETIEN > ENTRETIEN LAVE-VAISSELLE")))
				.isEqualTo(ProductNature.ACCESSORY);
	}

	@Test
	void sparePartWinsOverAccessory() throws Exception {
		assertThat(natureOf(product("darty.com",
				"TV - VIDEO - SON > ACCESSOIRES TV - VIDEO - SON > PIECE DETACHEE TV - VIDEO - SON")))
				.isEqualTo(ProductNature.SPARE_PART);
		assertThat(natureOf(product("cdiscount.com", "ACCESSOIRES LAVE-LINGE", "darty.com",
				"PIECES DETACHEES LAVE-LINGE > JOINT HUBLOT")))
				.isEqualTo(ProductNature.SPARE_PART);
	}

	@Test
	void realWashingMachineHasNoNature() throws Exception {
		final Product p = product("darty.com",
				"GROS ELECTROMENAGER | GROS ELECTROMENAGER>LAVE-LINGE>LAVE-LINGE HUBLOT | LAVE-LINGE | GROS ELECTROMENAGER");

		assertThat(natureOf(p)).isNull();
		assertThat(p.getVertical()).isEqualTo("washing-machine");
	}

	@Test
	void productWithoutCategoryHasNoNature() throws Exception {
		assertThat(natureOf(new Product())).isNull();
	}

	/**
	 * Hoover HLP610BL10UH47 (8059019130255): EPREL keeps it in washing-machine
	 * although aosom categorises it as a shoe-care accessory. A product with a
	 * vertical never gets a nature, else demoteAccessories (4e.3) would push a real
	 * washing machine down its own group.
	 */
	@Test
	void eprelConfirmedApplianceWithAnAccessoryCategoryHasNoNature() throws Exception {
		final Product p = product("darty.com",
				"GROS ELECTROMENAGER | GROS ELECTROMENAGER>LAVE-LINGE>LAVE-LINGE HUBLOT | LAVE-LINGE | GROS ELECTROMENAGER",
				"aosom.fr",
				"MAISON ET JARDIN > PRODUITS MENAGERS > ENTRETIEN ET ACCESSOIRES POUR CHAUSSURES > KITS D'ENTRETIEN DE CHAUSSURES");
		final EprelProduct eprel = new EprelProduct();
		eprel.setProductGroup("washingmachines2019");
		p.setEprelDatas(eprel);

		assertThat(natureOf(p)).isNull();
		assertThat(p.getVertical()).isEqualTo("washing-machine");
	}

	/**
	 * The Hoover aosom category loses its marker to the neutral fragment
	 * {@code ET ACCESSOIRES}; this one keeps it (4e.0: 24 washing machines under
	 * this category), so only the vertical rule keeps the nature absent.
	 */
	@Test
	void eprelConfirmedApplianceWithAnAccessoryMarkerHasNoNature() throws Exception {
		final Product p = product("darty.com",
				"GROS ELECTROMENAGER | GROS ELECTROMENAGER>LAVE-LINGE>LAVE-LINGE HUBLOT | LAVE-LINGE | GROS ELECTROMENAGER",
				"cdiscount.com", "ELECTROMENAGER > ACCESSOIRES POUR LAVE-LINGE ET SECHE-LINGE");
		final EprelProduct eprel = new EprelProduct();
		eprel.setProductGroup("washingmachines2019");
		p.setEprelDatas(eprel);

		assertThat(verticalService.getNatureForCategories(p.getCategoriesByDatasources().values()))
				.as("the categories alone carry an accessory marker").isEqualTo(ProductNature.ACCESSORY);
		assertThat(natureOf(p)).isNull();
		assertThat(p.getVertical()).isEqualTo("washing-machine");
	}

	/** Same rule on the branch that keeps an EPREL-confirmed vertical no category matches any more. */
	@Test
	void keptEprelConfirmedVerticalHasNoNature() throws Exception {
		final Product p = product("cdiscount.com", "ACCESSOIRES LAVE-LINGE");
		p.setVertical("washing-machine");
		final EprelProduct eprel = new EprelProduct();
		eprel.setProductGroup("washingmachines2019");
		p.setEprelDatas(eprel);
		p.setNature(ProductNature.ACCESSORY);

		assertThat(natureOf(p)).isNull();
		assertThat(p.getVertical()).isEqualTo("washing-machine");
	}

	@Test
	void neutralFragmentsAreNotMarkers() throws Exception {
		assertThat(natureOf(product("ldlc.com",
				"65 | INPUT DEVICES | INFORMATIQUE | INFORMATIQUE > COMPOSANT - PIECE DETACHEE > PROCESSEUR | COMPOSANT - PIECE DETACHEE | PROCESSEUR")))
				.as("processor").isNull();
		assertThat(natureOf(product("ldlc.com",
				"85 | MONITORS | INFORMATIQUE - ECRAN | APPAREILS ELECTRONIQUES > VIDEO > ACCESSOIRES VIDEO > ACCESSOIRES POUR TELEVISEURS")))
				.as("monitor").isNull();
		assertThat(natureOf(product("cdiscount.com",
				"TV - VIDEO - SON | TV - VIDEO - SON > TELEVISEUR > PACK TV LED ET ACCESSOIRES")))
				.as("TV pack").isNull();
	}

	@Test
	void maintenanceAloneIsNotAMarker() throws Exception {
		assertThat(natureOf(product("leroymerlin.fr",
				"ENTRETIEN DE LA MAISON > VENTILATION ET CLIMATISATION > CLIMATISEURS")))
				.isNull();
	}

	@Test
	void natureIsClearedWhenTheCategoryNoLongerCarriesAMarker() throws Exception {
		final Product p = product("darty.com", "PIECES DETACHEES LAVE-LINGE > BAC PRODUIT LAVE LINGE");
		assertThat(natureOf(p)).isEqualTo(ProductNature.SPARE_PART);

		p.getCategoriesByDatasources().put("darty.com", IdHelper.getCategoryName("DIVERS > AUTRES"));

		assertThat(natureOf(p)).isNull();
	}
}
