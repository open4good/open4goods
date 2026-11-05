package org.open4goods.api.services.aggregation.services.realtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.HashMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.open4goods.commons.exceptions.AggregationSkipException;
import org.open4goods.commons.services.textgen.BlablaService;
import org.open4goods.model.exceptions.InvalidParameterException;
import org.open4goods.model.attribute.ReferentielKey;
import org.open4goods.model.Localisable;
import org.open4goods.model.product.Product;
import org.open4goods.model.product.ProductTexts;
import org.open4goods.model.vertical.AttributeConfig;
import org.open4goods.model.vertical.AttributesConfig;
import org.open4goods.model.vertical.PrefixedAttrText;
import org.open4goods.model.vertical.ProductI18nElements;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.verticals.VerticalsConfigService;
import org.slf4j.LoggerFactory;

@ExtendWith(MockitoExtension.class)
class NamesAggregationServiceTest {

	@Mock
	private VerticalsConfigService verticalsConfigService;

	@Mock
	private BlablaService blablaService;

	private NamesAggregationService service;

	@BeforeEach
	void setUp() {
		service = new NamesAggregationService(
				LoggerFactory.getLogger(NamesAggregationService.class),
				verticalsConfigService,
				blablaService);
	}

	@Test
	void onProduct_shouldGenerateCanonicalNames() throws AggregationSkipException, InvalidParameterException {
		VerticalConfig config = buildVerticalConfig();
		when(verticalsConfigService.getConfigByIdOrDefault(any())).thenReturn(config);

		Product product = new Product(9L);
		product.setVertical("tv");
		product.getAttributes().addReferentielAttribute(ReferentielKey.BRAND, "Samsung");
		product.getAttributes().addReferentielAttribute(ReferentielKey.MODEL, "Galaxy TV");

		service.onProduct(product, config);

		assertThat(product.getNames().getDisplayName().get("fr")).isEqualTo("Samsung Galaxy TV");
		assertThat(product.getNames().getCardName().get("fr")).isEqualTo("Samsung Galaxy TV");
		assertThat(product.getNames().getPageTitle().get("fr")).isEqualTo("Samsung Galaxy TV");
		assertThat(product.getNames().getSeoName().get("fr")).isEqualTo("Samsung Galaxy TV");
	}



	private VerticalConfig buildVerticalConfig() {
		VerticalConfig config = new VerticalConfig();
		ProductI18nElements productI18nElements = new ProductI18nElements();
		productI18nElements.setPageTitle("Cuisine");


		HashMap<String, ProductI18nElements> i18n = new HashMap<>();
		i18n.put("fr", productI18nElements);
		config.setI18n(i18n);
		config.setAttributesConfig(buildAttributesConfig());
		return config;
	}

	private AttributesConfig buildAttributesConfig() {
		AttributeConfig diagonale = new AttributeConfig();
		diagonale.setKey("DIAGONALE_POUCES");
		Localisable<String, String> suffix = new Localisable<>();
		suffix.put("default", "\"");
		suffix.put("fr", "\"");
		diagonale.setSuffix(suffix);
		AttributesConfig attributesConfig = new AttributesConfig();
		attributesConfig.setConfigs(java.util.List.of(diagonale));
		return attributesConfig;
	}
	@Test
	void generateUrl_shouldIncludeGtinInUrl() throws AggregationSkipException, InvalidParameterException {
		VerticalConfig config = buildVerticalConfig();
		config.setId("tv");
		when(verticalsConfigService.getConfigByIdOrDefault(any())).thenReturn(config);
		when(blablaService.generateBlabla(anyString(), any())).thenReturn("TV");

		Product product = new Product(123456789L);
		product.setVertical("tv");
		// Ensure GTIN is present (id is used as GTIN in Product)
		
		org.open4goods.model.attribute.ProductAttribute attr = new org.open4goods.model.attribute.ProductAttribute();
		attr.setName("DIAGONALE_POUCES");
		attr.setValue("55");
		product.getAttributes().getAll().put("DIAGONALE_POUCES", attr);
		
		// Setup URL prefix config
		PrefixedAttrText urlConfig = new PrefixedAttrText();
		urlConfig.setPrefix("TV");
		urlConfig.setAttrs(java.util.List.of("DIAGONALE_POUCES"));
		config.getI18n().get("fr").setUrl(urlConfig);

		service.onProduct(product, config);

		String generatedUrl = product.getNames().getUrl().get("fr");
		assertThat(generatedUrl).startsWith("123456789-");
		assertThat(generatedUrl).contains("tv-55");
	}

	@Test
	void onProduct_shouldComputeTemplateTitles() throws AggregationSkipException, InvalidParameterException {
		VerticalConfig config = buildVerticalConfig();
		config.getI18n().get("fr").setCardName("{BRAND} {MODEL}");
		config.getI18n().get("fr").setDisplayName("{BRAND}");
		config.getI18n().get("fr").setPageTitle("{BRAND} {MODEL} - {DIAGONALE_POUCES} {ATTRIBUTE_NOT_EXIST}");

		when(verticalsConfigService.getConfigByIdOrDefault(any())).thenReturn(config);

		Product product = new Product(100L);
		product.setVertical("tv");
		product.getAttributes().addReferentielAttribute(ReferentielKey.BRAND, "Samsung");
		product.getAttributes().addReferentielAttribute(ReferentielKey.MODEL, "Galaxy TV");
		
		org.open4goods.model.attribute.ProductAttribute attr = new org.open4goods.model.attribute.ProductAttribute();
		attr.setName("DIAGONALE_POUCES");
		attr.setValue("55");
		product.getAttributes().getAll().put("DIAGONALE_POUCES", attr);

		service.onProduct(product, config);

		assertThat(product.getNames().getCardName().get("fr")).isEqualTo("Samsung Galaxy TV");
		assertThat(product.getNames().getDisplayName().get("fr")).isEqualTo("Samsung Galaxy TV");
		assertThat(product.getNames().getPageTitle().get("fr")).isEqualTo("Samsung Galaxy TV");
	}

	@Test
	void onProduct_shouldRejectLegacyRawTemplates() throws AggregationSkipException, InvalidParameterException {
		VerticalConfig config = buildVerticalConfig();
		config.getI18n().get("fr").setDisplayName("[(${p.brand()})] [(${p.model()})]");

		when(verticalsConfigService.getConfigByIdOrDefault(any())).thenReturn(config);

		Product product = new Product(101L);
		product.setVertical("tv");

		service.onProduct(product, config);

		assertThat(product.getNames().getDisplayName().get("fr")).isEqualTo("101");
	}
	@Test
	void onProduct_shouldLeaveLegacyTextEmbeddingUntouched()
			throws AggregationSkipException, InvalidParameterException {
		VerticalConfig config = buildVerticalConfig();
		when(verticalsConfigService.getConfigByIdOrDefault(any())).thenReturn(config);

		Product product = new Product(8L);
		product.setVertical("vertical-id");
		product.getAttributes().addReferentielAttribute(ReferentielKey.BRAND, "Marque");
		product.getAttributes().addReferentielAttribute(ReferentielKey.MODEL, "Modele");
		float[] legacyEmbedding = new float[] { 0.1f, 0.2f };
		product.setEmbedding(legacyEmbedding);
		product.setEmbeddingTextHash(123L);

		service.onProduct(product, config);

		assertThat(product.getEmbedding()).isSameAs(legacyEmbedding);
		assertThat(product.getEmbeddingTextHash()).isEqualTo(123L);
	}

	@Test
	void onProduct_shouldKeepFrozenSlugWhenNameOrAttributesChange()
			throws AggregationSkipException, InvalidParameterException {
		VerticalConfig config = buildSlugConfig("tv");
		when(verticalsConfigService.getConfigByIdOrDefault(any())).thenReturn(config);

		Product product = new Product(8431312260509L);
		product.setVertical("tv");
		product.getAttributes().addReferentielAttribute(ReferentielKey.BRAND, "Midea");
		product.getAttributes().addReferentielAttribute(ReferentielKey.MODEL, "MB55");
		product.getNames().getUrl().put("fr", "8431312260509-tv-midea-55");
		product.getNames().setUrlVertical("tv");
		product.getNames().getDisplayName().put("fr", "Ancien nom");
		putAttribute(product, "DIAGONALE_POUCES", "65");

		service.onProduct(product, config);

		assertThat(product.getNames().getUrl().get("fr")).isEqualTo("8431312260509-tv-midea-55");
		assertThat(product.getNames().getUrlVertical()).isEqualTo("tv");
		// forceNameGeneration still rewrites titles and meta
		assertThat(product.getNames().getDisplayName().get("fr")).isEqualTo("Midea MB55");
	}

	@Test
	void onProduct_shouldFreezeLegacySlugAndRecordItsVertical()
			throws AggregationSkipException, InvalidParameterException {
		VerticalConfig config = buildSlugConfig("tv");
		when(verticalsConfigService.getConfigByIdOrDefault(any())).thenReturn(config);

		Product product = new Product(123456789L);
		product.setVertical("tv");
		product.getNames().getUrl().put("fr", "123456789-ancien-slug");
		putAttribute(product, "DIAGONALE_POUCES", "55");

		service.onProduct(product, config);

		assertThat(product.getNames().getUrl().get("fr")).isEqualTo("123456789-ancien-slug");
		assertThat(product.getNames().getUrlVertical()).isEqualTo("tv");
	}

	@Test
	void onProduct_shouldRegenerateSlugWhenVerticalChanged()
			throws AggregationSkipException, InvalidParameterException {
		VerticalConfig config = buildSlugConfig("tv");
		when(verticalsConfigService.getConfigByIdOrDefault(any())).thenReturn(config);
		when(blablaService.generateBlabla(anyString(), any())).thenReturn("TV");

		Product product = new Product(123456789L);
		product.setVertical("tv");
		product.getNames().getUrl().put("fr", "123456789-ecran-pc-55");
		product.getNames().setUrlVertical("monitor");
		putAttribute(product, "DIAGONALE_POUCES", "55");

		service.onProduct(product, config);

		assertThat(product.getNames().getUrl().get("fr")).isEqualTo("123456789-tv-55");
		assertThat(product.getNames().getUrlVertical()).isEqualTo("tv");
	}

	@Test
	void onProduct_shouldRegenerateSlugWhenProductGainsVertical()
			throws AggregationSkipException, InvalidParameterException {
		VerticalConfig config = buildSlugConfig("tv");
		when(verticalsConfigService.getConfigByIdOrDefault(any())).thenReturn(config);
		when(blablaService.generateBlabla(anyString(), any())).thenReturn("TV");

		Product product = new Product(123456789L);
		product.setVertical("tv");
		product.getNames().getUrl().put("fr", "123456789-nom-offre");
		product.getNames().setUrlVertical(ProductTexts.URL_WITHOUT_VERTICAL);
		putAttribute(product, "DIAGONALE_POUCES", "55");

		service.onProduct(product, config);

		assertThat(product.getNames().getUrl().get("fr")).isEqualTo("123456789-tv-55");
		assertThat(product.getNames().getUrlVertical()).isEqualTo("tv");
	}

	@Test
	void onProduct_shouldGenerateMissingSlugAndRecordVertical()
			throws AggregationSkipException, InvalidParameterException {
		VerticalConfig config = buildSlugConfig("tv");
		when(verticalsConfigService.getConfigByIdOrDefault(any())).thenReturn(config);
		when(blablaService.generateBlabla(anyString(), any())).thenReturn("TV");

		Product product = new Product(123456789L);
		product.setVertical("tv");
		putAttribute(product, "DIAGONALE_POUCES", "55");

		service.onProduct(product, config);

		assertThat(product.getNames().getUrl().get("fr")).isEqualTo("123456789-tv-55");
		assertThat(product.getNames().getUrlVertical()).isEqualTo("tv");
	}

	@Test
	void onProduct_shouldKeepSlugWhenVerticalIsLostThenRestored()
			throws AggregationSkipException, InvalidParameterException {
		VerticalConfig defaultConfig = buildSlugConfig(null);
		VerticalConfig tvConfig = buildSlugConfig("tv");
		when(verticalsConfigService.getConfigByIdOrDefault(any())).thenReturn(defaultConfig, tvConfig);

		Product product = new Product(123456789L);
		product.getNames().getUrl().put("fr", "123456789-tv-55");
		product.getNames().setUrlVertical("tv");
		product.getOfferNames().add("Offre");

		service.onProduct(product, defaultConfig);

		assertThat(product.getNames().getUrl().get("fr")).isEqualTo("123456789-tv-55");
		assertThat(product.getNames().getUrlVertical()).isEqualTo("tv");

		product.setVertical("tv");
		service.onProduct(product, tvConfig);

		assertThat(product.getNames().getUrl().get("fr")).isEqualTo("123456789-tv-55");
		assertThat(product.getNames().getUrlVertical()).isEqualTo("tv");
	}

	@Test
	void onProduct_shouldRecordMissingVerticalForSlugGeneratedWithoutVertical()
			throws AggregationSkipException, InvalidParameterException {
		VerticalConfig defaultConfig = buildSlugConfig(null);
		when(verticalsConfigService.getConfigByIdOrDefault(any())).thenReturn(defaultConfig);

		Product product = new Product(123456789L);
		product.getOfferNames().add("Offre courte");

		service.onProduct(product, defaultConfig);

		assertThat(product.getNames().getUrl().get("fr")).isEqualTo("123456789-offre-courte");
		assertThat(product.getNames().getUrlVertical()).isEqualTo(ProductTexts.URL_WITHOUT_VERTICAL);
	}

	private VerticalConfig buildSlugConfig(String verticalId) {
		VerticalConfig config = buildVerticalConfig();
		config.setId(verticalId);
		config.setForceNameGeneration(true);
		config.getI18n().get("fr").setDisplayName("{BRAND} {MODEL} {DIAGONALE_POUCES}");
		PrefixedAttrText urlConfig = new PrefixedAttrText();
		urlConfig.setPrefix("TV");
		urlConfig.setAttrs(java.util.List.of("DIAGONALE_POUCES"));
		config.getI18n().get("fr").setUrl(urlConfig);
		return config;
	}

	private void putAttribute(Product product, String key, String value) {
		org.open4goods.model.attribute.ProductAttribute attr = new org.open4goods.model.attribute.ProductAttribute();
		attr.setName(key);
		attr.setValue(value);
		product.getAttributes().getAll().put(key, attr);
	}

}
