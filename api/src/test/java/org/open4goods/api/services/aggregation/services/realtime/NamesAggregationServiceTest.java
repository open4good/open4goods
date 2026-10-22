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

}
