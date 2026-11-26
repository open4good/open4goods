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
 * {@link TaxonomyRealTimeAggregationService#onProduct} with the vote between datasources switched on (GOU-2256),
 * against the real vertical configurations. The flag is set on a dedicated {@link VerticalsConfigService}: the
 * other tests keep {@code _default.yml} as shipped. Categories are reconstructed from the card, except the Zalman P10
 * case, copied from {@code products-moustik} (the real categories of the card are replayed in {@code CategoryVoteTest}
 * of the verticals module).
 */
class TaxonomyRealTimeAggregationServiceCategoryVoteTest {

	private static final String RAKUTEN_WASHING_MACHINE = "ELECTROMENAGER | LAVE-LINGE";
	private static final String DARTY_SPARE_PART = "PIECES DETACHEES LAVE-LINGE > BAC PRODUIT LAVE LINGE";
	private static final String AOSOM_SHOE_CARE = "MAISON ET JARDIN > PRODUITS MENAGERS > ENTRETIEN ET ACCESSOIRES "
			+ "POUR CHAUSSURES > KITS D'ENTRETIEN DE CHAUSSURES";

	private static TaxonomyRealTimeAggregationService service;

	@BeforeAll
	static void setUp() {
		VerticalsConfigService verticalService = new VerticalsConfigService(new SerialisationService(),
				mock(GoogleTaxonomyService.class), new PathMatchingResourcePatternResolver());
		verticalService.getDefaultConfig().setCategoryVoteEnabled(true);
		service = new TaxonomyRealTimeAggregationService(
				LoggerFactory.getLogger(TaxonomyRealTimeAggregationServiceCategoryVoteTest.class), verticalService);
	}

	/** Builds a product from raw feed categories, normalised as {@code DataFragment} does. */
	private static Product product(final String vertical, final String... datasourceAndCategory) {
		final Product p = new Product();
		p.setVertical(vertical);
		for (int i = 0; i < datasourceAndCategory.length; i += 2) {
			p.getCategoriesByDatasources().put(datasourceAndCategory[i],
					IdHelper.getCategoryName(datasourceAndCategory[i + 1]));
		}
		return p;
	}

	private static void aggregate(final Product p) throws Exception {
		service.onProduct(p, mock(VerticalConfig.class));
	}

	/**
	 * G.Skill RAM 4713294230263, in dishwasher from one marketplace row: the PC component row outvotes it. The nature
	 * of 4e is unchanged: {@code COMPOSANT - PIECE DETACHEE} is a neutral fragment there, so no nature.
	 */
	@Test
	void falsePositiveLosesItsVerticalAndKeepsTheNatureOf4e() throws Exception {
		final Product p = product("dishwasher",
				"rakuten.com-csv.yml", "ELECTROMENAGER | LAVE-VAISSELLE",
				"alternate FR", "INFORMATIQUE > COMPOSANT - PIECE DETACHEE > MEMOIRE PC");
		p.setGoogleTaxonomyId(1);

		aggregate(p);

		assertThat(p.getVertical()).isNull();
		assertThat(p.getGoogleTaxonomyId()).isNull();
		assertThat(p.getNature()).isNull();
	}

	/**
	 * Zalman P10 8800263650385 (GOU-2317), real categories: one refrigerator row for, two PC component rows without
	 * {@code PIECE DETACHEE} against since GOU-2317. It loses refrigerator, and gets no nature: the PC component markers
	 * are neutral fragments of the nature.
	 */
	@Test
	void pcCaseFiledAsAFridgeLosesItsVerticalWithoutNature() throws Exception {
		final Product p = product("refrigerator",
				"RueDuCommerce FR", "BOITIER PC",
				"rakuten.com-csv.yml", "INFORMATIQUE | COMPOSANTS | BOITIER",
				"Cdiscount FR", "ELECTROMENAGER | ELECTROMENAGER > FROID > ARMOIRE A BOISSON - ARRIERE-BAR - DESSOUS DE "
						+ "COMPTOIR | FROID | ARMOIRE A BOISSON - ARRIERE-BAR - DESSOUS DE COMPTOIR");
		p.setGoogleTaxonomyId(1);

		aggregate(p);

		assertThat(p.getVertical()).isNull();
		assertThat(p.getGoogleTaxonomyId()).isNull();
		assertThat(p.getNature()).isNull();
	}

	@Test
	void sparePartLeftWithoutVerticalGetsItsNature() throws Exception {
		final Product p = product(null,
				"rakuten.com-csv.yml", RAKUTEN_WASHING_MACHINE,
				"Darty FR", DARTY_SPARE_PART);

		aggregate(p);

		assertThat(p.getVertical()).isNull();
		assertThat(p.getNature()).isEqualTo(ProductNature.SPARE_PART);
	}

	@Test
	void realPhoneWithOneComponentRowKeepsItsVertical() throws Exception {
		final Product p = product(null,
				"Cdiscount FR", "TELEPHONIE ET MOBILITE / TELEPHONE MOBILE / SMARTPHONE | TELEPHONIE ET MOBILITE>"
						+ "TELEPHONE MOBILE>SMARTPHONE | TELEPHONE MOBILE | SMARTPHONE",
				"Fnac FR", "348 | MOBILE PHONES | SYSTEME PC & APPAREIL PORTABLE > APPAREIL MOBILE > TELEPHONE MOBILE",
				"rakuten.com-csv.yml", "INFORMATIQUE > COMPOSANT - PIECE DETACHEE > PROCESSEUR");

		aggregate(p);

		assertThat(p.getVertical()).isEqualTo("smartphones");
		assertThat(p.getNature()).isNull();
	}

	/** The EPREL group of the vertical wins over the votes, in the vote and in the fallback of the service. */
	@Test
	void eprelConfirmedProductKeepsItsVertical() throws Exception {
		final EprelProduct eprel = new EprelProduct();
		eprel.setProductGroup("washingmachines2019");

		final Product outvoted = product(null,
				"rakuten.com-csv.yml", RAKUTEN_WASHING_MACHINE,
				"Darty FR", DARTY_SPARE_PART,
				"aosom.fr", AOSOM_SHOE_CARE);
		outvoted.setEprelDatas(eprel);
		aggregate(outvoted);
		assertThat(outvoted.getVertical()).isEqualTo("washing-machine");
		assertThat(outvoted.getNature()).isNull();

		// No row matches the vertical any more: the vote gives nothing, the service keeps the current one
		final Product unmatched = product("washing-machine",
				"Darty FR", DARTY_SPARE_PART,
				"aosom.fr", AOSOM_SHOE_CARE);
		unmatched.setEprelDatas(eprel);
		aggregate(unmatched);
		assertThat(unmatched.getVertical()).isEqualTo("washing-machine");
		assertThat(unmatched.getNature()).isNull();

		// Two rows vote for dishwasher: the EPREL group of washing-machine stops the vote from moving the product
		final Product contradicted = product("washing-machine",
				"Cdiscount FR", "ELECTROMENAGER | ELECTROMENAGER > LAVAGE-SECHAGE > LAVE-VAISSELLE | LAVAGE-SECHAGE | "
						+ "LAVE-VAISSELLE",
				"rakuten.com-csv.yml", "ELECTROMENAGER | LAVE-VAISSELLE",
				"aosom.fr", AOSOM_SHOE_CARE);
		contradicted.setEprelDatas(eprel);
		aggregate(contradicted);
		assertThat(contradicted.getVertical()).isEqualTo("washing-machine");
		assertThat(contradicted.getNature()).isNull();
	}
}
