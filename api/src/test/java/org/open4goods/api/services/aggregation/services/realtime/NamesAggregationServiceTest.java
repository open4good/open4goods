package org.open4goods.api.services.aggregation.services.realtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.HashMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.open4goods.embedding.service.DjlTextEmbeddingService;
import org.open4goods.embedding.config.DjlEmbeddingProperties;
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
import org.open4goods.services.evaluation.service.EvaluationService;
import org.open4goods.verticals.VerticalsConfigService;
import org.slf4j.LoggerFactory;

@ExtendWith(MockitoExtension.class)
class NamesAggregationServiceTest {

	@Mock
	private VerticalsConfigService verticalsConfigService;

	@Mock
	private EvaluationService evaluationService;

	@Mock
	private BlablaService blablaService;

	@Mock
	private DjlTextEmbeddingService embeddingService;

	private DjlEmbeddingProperties embeddingProperties;

	private NamesAggregationService service;

	@BeforeEach
	void setUp() {
		embeddingProperties = new DjlEmbeddingProperties();
		service = new NamesAggregationService(
				LoggerFactory.getLogger(NamesAggregationService.class),
				verticalsConfigService,
				evaluationService,
				blablaService,
				embeddingService,
				embeddingProperties);
	}

	@Test
	void buildEmbeddingText_shouldBlendBrandModelOffersAndVertical() {
		VerticalConfig config = buildVerticalConfig();
		Product product = new Product(42L);
		product.getAttributes().addReferentielAttribute(ReferentielKey.BRAND, "Marque");
		product.getAttributes().addReferentielAttribute(ReferentielKey.MODEL, "Modele");
		product.getOfferNames().add("premiere offre");
		product.getOfferNames().add("seconde offre");

		String combined = service.buildEmbeddingText(product, config);

		assertThat(combined)
				.contains("Marque")
				.contains("Modele")
				.contains("premiere offre")
				.contains("Cuisine");
		assertThat(combined.length()).isLessThanOrEqualTo(1000);
	}

	@Test
	void onProduct_shouldStoreEmbeddingWhenTextPresent() throws AggregationSkipException, InvalidParameterException {
		VerticalConfig config = buildVerticalConfig();
		when(verticalsConfigService.getConfigByIdOrDefault(any())).thenReturn(config);
		when(blablaService.generateBlabla(anyString(), any())).thenReturn("pref");
		when(embeddingService.embed(anyString())).thenReturn(new float[] { 0.1f, 0.2f });

		Product product = new Product(7L);
		product.setVertical("vertical-id");
		product.getAttributes().addReferentielAttribute(ReferentielKey.BRAND, "Marque");
		product.getAttributes().addReferentielAttribute(ReferentielKey.MODEL, "Modele");
		product.getOfferNames().add("offre");

		service.onProduct(product, config);

		assertNotNull(product.getEmbedding());
	}

	@Test
	void onProduct_shouldComputePrettyNameWithSuffix() throws AggregationSkipException, InvalidParameterException {
		VerticalConfig config = buildVerticalConfig();
		when(verticalsConfigService.getConfigByIdOrDefault(any())).thenReturn(config);
		when(blablaService.generateBlabla(anyString(), any())).thenReturn("TV");

		Product product = new Product(9L);
		product.setVertical("tv");
		org.open4goods.model.attribute.ProductAttribute attr = new org.open4goods.model.attribute.ProductAttribute();
		attr.setName("DIAGONALE_POUCES");
		attr.setValue("55");
		product.getAttributes().getAll().put("DIAGONALE_POUCES", attr);

		service.onProduct(product, config);

		assertThat(product.getNames().getPrettyName().get("fr"))
				.isEqualTo("TV 55 \"");
	}

	private VerticalConfig buildVerticalConfig() {
		VerticalConfig config = new VerticalConfig();
		ProductI18nElements productI18nElements = new ProductI18nElements();
		PrefixedAttrText h1 = new PrefixedAttrText();
		h1.setPrefix("Cuisine");
		productI18nElements.setH1Title(h1);
		PrefixedAttrText pretty = new PrefixedAttrText();
		pretty.setPrefix("Cuisine");
		pretty.setAttrs(java.util.List.of("DIAGONALE_POUCES"));
		productI18nElements.setPrettyName(pretty);

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
}
