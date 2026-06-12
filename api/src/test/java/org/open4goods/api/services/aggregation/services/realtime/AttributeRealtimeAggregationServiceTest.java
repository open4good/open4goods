package org.open4goods.api.services.aggregation.services.realtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Method;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.open4goods.brand.model.Brand;
import org.open4goods.brand.service.BrandService;
import org.open4goods.icecat.services.IcecatFeatureResolver;
import org.open4goods.model.attribute.Attribute;
import org.open4goods.model.attribute.ProductAttribute;
import org.open4goods.model.attribute.ReferentielKey;
import org.open4goods.model.datafragment.DataFragment;
import org.open4goods.model.eprel.EprelProduct;
import org.open4goods.model.exceptions.ValidationException;
import org.open4goods.model.product.Product;
import org.open4goods.model.vertical.AttributeConfig;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.verticals.VerticalsConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class AttributeRealtimeAggregationServiceTest {

        private AttributeRealtimeAggregationService service;
        private VerticalConfig verticalConfig;
        private IcecatFeatureResolver icecatFeatureResolver;
        private BrandService brandService;

        @BeforeEach
        void setUp() {
                Logger logger = LoggerFactory.getLogger(AttributeRealtimeAggregationServiceTest.class);
                brandService = Mockito.mock(BrandService.class);
                Mockito.when(brandService.resolve(Mockito.anyString(), Mockito.anyMap()))
                                .thenAnswer(invocation -> new Brand(invocation.getArgument(0, String.class).toUpperCase()));
                VerticalsConfigService verticalConfigService = Mockito.mock(VerticalsConfigService.class);
                icecatFeatureResolver = Mockito.mock(IcecatFeatureResolver.class);
                service = new AttributeRealtimeAggregationService(verticalConfigService, brandService, logger, icecatFeatureResolver);

                verticalConfig = new VerticalConfig();
                verticalConfig.setRequiredAttributes(Set.of("required_attr"));
        }

        @Test
        void updateExcludeStatusDoesNotExcludeWhenRequiredAttributesPresent() throws Exception {
                Product product = buildBaseProduct();
                addAttribute(product, "required_attr");

                invokeUpdateExcludeStatus(product);

                assertThat(product.isExcluded()).isFalse();
                assertThat(product.getExcludedCauses()).isEmpty();
        }

        @Test
        void updateExcludeStatusFlagsMissingRequiredAttributes() throws Exception {
                Product product = buildBaseProduct();

                invokeUpdateExcludeStatus(product);

                assertThat(product.isExcluded()).isTrue();
                assertThat(product.getExcludedCauses()).containsExactly("missing_attr_required_attr");
        }

        @Test
        void handleReferentielAttributesCapturesRawGtin() throws Exception {
                Product product = new Product(123456789012L);
                DataFragment fragment = new DataFragment();
                fragment.getReferentielAttributes().put(ReferentielKey.GTIN, "0123456789012");

                Method method = AttributeRealtimeAggregationService.class.getDeclaredMethod("handleReferentielAttributes", DataFragment.class, Product.class, VerticalConfig.class);
                method.setAccessible(true);
                method.invoke(service, fragment, product, verticalConfig);

                assertThat(product.getGtinInfos().getGtinStrings()).containsExactly("0123456789012");
        }

        @Test
        void onProductAssignsIcecatTaxonomyIdsFromResolver() throws Exception {
                Product product = buildBaseProduct();
                addAttribute(product, "COULEUR");
                Mockito.when(icecatFeatureResolver.resolveFeatureName("COULEUR")).thenReturn(Set.of(46, 46757));

                service.onProduct(product, verticalConfig);

                assertThat(product.getAttributes().getAll().get("COULEUR").getIcecatTaxonomyIds())
                                .containsExactlyInAnyOrder(46, 46757);
        }

        @Test
        void onProductStoresCanonicalBrandAcrossPartnerAliases() throws Exception {
                Product product = new Product(123L);
                product.addBrand("partner", "LG Electronics Inc.", null, null);
                product.addBrand("icecat", "LG Electronics", null, null);
                Brand lg = new Brand("LG");
                lg.setCompanyName("LG Electronics, Inc.");
                Mockito.when(brandService.resolve(Mockito.startsWith("LG"), Mockito.anyMap())).thenReturn(lg);

                service.onProduct(product, verticalConfig);

                assertThat(product.brand()).isEqualTo("LG");
                assertThat(product.getAkaBrands().values()).containsOnly("LG");
        }

        private Product buildBaseProduct() {
                Product product = new Product(123L);
                product.getAttributes().getReferentielAttributes().put(ReferentielKey.BRAND, "Brand");
                product.getAttributes().getReferentielAttributes().put(ReferentielKey.MODEL, "ModelX");
                EprelProduct eprelProduct = new EprelProduct();
                eprelProduct.setSupplierOrTrademark("Brand");
                eprelProduct.setModelIdentifier("ModelX");
                product.setEprelDatas(eprelProduct);
                return product;
        }

        private void addAttribute(Product product, String name) {
                ProductAttribute attribute = new ProductAttribute();
                attribute.setName(name);
                attribute.setValue("value");
                product.getAttributes().getAll().put(name, attribute);
        }

        private void invokeUpdateExcludeStatus(Product product) throws Exception {
                Method method = AttributeRealtimeAggregationService.class.getDeclaredMethod("updateExcludeStatus", Product.class, VerticalConfig.class);
                method.setAccessible(true);
                method.invoke(service, product, verticalConfig);
        }
	@Test
	void testSynonymsResolution() throws Exception {
		// Arrange
		AttributeConfig attrConfig = new AttributeConfig();
		attrConfig.setKey("ENERGY_CLASS");
		attrConfig.getParser().setUpperCase(true);
		attrConfig.getParser().setDeleteTokens(java.util.List.of("CLASSE"));
		attrConfig.getParser().setNormalize(true);
		attrConfig.getParser().setTrim(true);

		ProductAttribute attr = new ProductAttribute();
		attr.setName("ENERGY_CLASS");
		
		org.open4goods.model.attribute.SourcedAttribute source1 = new org.open4goods.model.attribute.SourcedAttribute();
		source1.setDataSourcename("source1");
		source1.setValue("Classe F");
		source1.setName("ENERGY_CLASS");
		
		org.open4goods.model.attribute.SourcedAttribute source2 = new org.open4goods.model.attribute.SourcedAttribute();
		source2.setDataSourcename("source2");
		source2.setValue("F");
		source2.setName("ENERGY_CLASS");
		
		attr.addSourceAttribute(source1);
		attr.addSourceAttribute(source2);
		
		org.open4goods.model.attribute.IndexedAttribute indexedAttr = new org.open4goods.model.attribute.IndexedAttribute("ENERGY_CLASS", "F");

		// Act
		Method method = AttributeRealtimeAggregationService.class.getDeclaredMethod("mergeSourcesAndRefreshValue", org.open4goods.model.attribute.IndexedAttribute.class, ProductAttribute.class, AttributeConfig.class, VerticalConfig.class);
		method.setAccessible(true);
		method.invoke(service, indexedAttr, attr, attrConfig, verticalConfig);

		// Assert
		assertThat(indexedAttr.hasConflicts()).isFalse();
		assertThat(indexedAttr.distinctValues()).isEqualTo(1);
		assertThat(indexedAttr.getValue()).isEqualTo("F");
		
		// Check that source attributes have cleaned values
		assertThat(attr.getSource()).anySatisfy(s -> {
			if (s.getDataSourcename().equals("source1")) {
				assertThat(s.getCleanedValue()).isEqualTo("F");
			}
		});
	}


	@Test
	void testDeleteTokensWithUpperCase() throws Exception {
		// Arrange
		AttributeConfig attrConfig = new AttributeConfig();
		attrConfig.setKey("DISPLAY_TECHNOLOGY");
		attrConfig.getParser().setUpperCase(true);
		attrConfig.getParser().setDeleteTokens(java.util.List.of("TV", "_LCD"));
		attrConfig.getParser().setNormalize(true);
		attrConfig.getParser().setTrim(true);

		// Act
		String result = service.parseValue("LED_LCD", attrConfig, verticalConfig);

		// Assert
		assertThat(result).isEqualTo("LED");
	}

	@Test
	void testDeleteTokensWithCaseMismatch() throws Exception {
		// Arrange
		AttributeConfig attrConfig = new AttributeConfig();
		attrConfig.setKey("DISPLAY_TECHNOLOGY");
		attrConfig.getParser().setUpperCase(true);
		// Token is mixed case, but input will be uppercased. 
		// Ideally the parser should automatically handle the token casing to match the parser config.
		attrConfig.getParser().setDeleteTokens(java.util.List.of("_Lcd"));
		attrConfig.getParser().setNormalize(true);
		attrConfig.getParser().setTrim(true);

		// Act
		// Input "LED_LCD". 
		// 1. UpperCase -> "LED_LCD"
		// 2. Delete "_Lcd" -> Should delete "_LCD" if logic is robust
		String result = service.parseValue("LED_LCD", attrConfig, verticalConfig);

		// Assert
		assertThat(result).isEqualTo("LED");
	}

	@Test
	void testPowerConsumptionOffParsing() throws Exception {
		// Arrange
		AttributeConfig attrConfig = new AttributeConfig();
		attrConfig.setKey("POWER_CONSUMPTION_OFF");
		attrConfig.getParser().setUpperCase(true);
		attrConfig.getParser().setDeleteTokens(java.util.List.of("W"));
		
		// Act
		String result = service.parseValue("0.5 W", attrConfig, verticalConfig);
		// Assert
		assertThat(result).isEqualTo("0.5");
	}

	@Test
	void onDataFragmentMovesDescriptionAttributesToDatasourceDescriptions() throws Exception {
		// Arrange
		Product product = new Product(123L);
		DataFragment fragment = new DataFragment();
		fragment.setDatasourceName("source-a");
		fragment.getAttributes().add(new Attribute("DESCRIPTION", "Long description"));
		fragment.getAttributes().add(new Attribute("required_attr", "value"));
		verticalConfig.setDescriptionAttributes(Set.of("DESCRIPTION"));

		// Act
		service.onDataFragment(fragment, product, verticalConfig);

		// Assert
		assertThat(product.getDescriptionsByDatasource())
				.containsEntry("source-a", "Long description");
		assertThat(product.getAttributes().getAll()).containsKey("required_attr");
		assertThat(product.getAttributes().getAll()).doesNotContainKey("DESCRIPTION");
	}
	@Test
	void testColorMappingIdentity() throws Exception {
		// Arrange
		AttributeConfig attrConfig = new AttributeConfig();
		attrConfig.setKey("COLOR");
		attrConfig.getParser().setUpperCase(true);
		attrConfig.getParser().setTokenMatch(java.util.List.of("NOIR", "BLANC"));
		
		// Simulate the fixed state where mappings include identity for French values
		attrConfig.getMappings().put("BLACK", "NOIR");
		attrConfig.getMappings().put("WHITE", "BLANC");
		attrConfig.getMappings().put("NOIR", "NOIR");
		attrConfig.getMappings().put("BLANC", "BLANC");
		
		// Act
		String resultNoir = service.parseValue("NOIR", attrConfig, verticalConfig);
		String resultBlanc = service.parseValue("BLANC", attrConfig, verticalConfig);

		// Assert
		assertThat(resultNoir).isEqualTo("NOIR"); 
		assertThat(resultBlanc).isEqualTo("BLANC");
	}

	@Test
	void parseValueAppliesMappingsBeforeTokenMatch() throws Exception {
		AttributeConfig attrConfig = new AttributeConfig();
		attrConfig.setKey("COLOR");
		attrConfig.getParser().setUpperCase(true);
		attrConfig.getParser().setNormalize(true);
		attrConfig.getParser().setTrim(true);
		attrConfig.getParser().setTokenMatch(java.util.List.of("NOIR", "BLANC", "MULTICOLORE"));
		attrConfig.getMappings().put("BLACK", "NOIR");
		attrConfig.getMappings().put("MULTICOULEUR", "MULTICOLORE");

		assertThat(service.parseValue("Black", attrConfig, verticalConfig)).isEqualTo("NOIR");
		assertThat(service.parseValue("Multicouleur", attrConfig, verticalConfig)).isEqualTo("MULTICOLORE");
	}

	@Test
	void parseValueTreatsFalsePlaceholderAsEmpty() throws Exception {
		AttributeConfig attrConfig = new AttributeConfig();
		attrConfig.setKey("DIAGONALE_POUCES");
		attrConfig.setFilteringType(org.open4goods.model.attribute.AttributeType.NUMERIC);
		attrConfig.getParser().setUpperCase(true);
		attrConfig.getParser().setNormalize(true);
		attrConfig.getParser().setTrim(true);

		assertThat(service.parseValue("false", attrConfig, verticalConfig)).isEmpty();
	}

	@Test
	void parseValueRejectsNullRawValueWithValidationException() {
		AttributeConfig attrConfig = new AttributeConfig();
		attrConfig.setKey("COLOR");

		assertThatThrownBy(() -> service.parseValue(null, attrConfig, verticalConfig))
				.isInstanceOf(ValidationException.class)
				.hasMessageContaining("Null rawValue in attribute COLOR");
	}

	@Test
	void onProductKeepsTrustedSourceValueWhenDuplicateAttributesMapToSameIndexedKey() throws Exception {
		AttributeConfig attrConfig = new AttributeConfig();
		attrConfig.setKey("COLOR");
		attrConfig.getSynonyms().put("all", Set.of("color_a", "color_b"));
		verticalConfig.getAttributesConfig().setConfigs(java.util.List.of(attrConfig));

		Product product = buildBaseProduct();
		addSourcedAttribute(product, "color_a", "black", "eprel");
		addSourcedAttribute(product, "color_b", "white", "merchant");

		service.onProduct(product, verticalConfig);

		assertThat(product.getAttributes().getIndexed()).containsKey("COLOR");
		assertThat(product.getAttributes().getIndexed().get("COLOR").getValue()).isEqualTo("BLACK");
		assertThat(product.getAttributes().getIndexed().get("COLOR").sourcesCount()).isEqualTo(2);
	}

	@Test
	void extractModelFromTitlesAddsBestTitleCandidateAsAlternateWhenModelAlreadyExists() {
		Product product = buildBaseProduct();
		product.getOfferNames().add("Samsung HG32EJ690WE television");

		service.extractModelFromTitles(product);

		assertThat(product.model()).isEqualTo("ModelX");
		assertThat(product.getAkaModels()).contains("HG32EJ690WE");
	}

	@Test
	void extractModelFromTitlesPromotesBestWhenModelEmpty() {
		Product product = new Product(123L);
		product.getOfferNames().add("Bosch SMV4HVX31E lave-vaisselle integrable");

		service.extractModelFromTitles(product);

		assertThat(product.model()).isEqualTo("SMV4HVX31E");
	}

	@Test
	void extractModelFromTitlesDoesNotPromoteFalsePositive() {
		Product product = new Product(123L);
		product.getOfferNames().add("Televiseur 55POUCES 144HZ");

		service.extractModelFromTitles(product);

		assertThat(product.model()).isNullOrEmpty();
	}

	@Test
	void extractModelFromTitlesAlternateIsCleanedUppercased() {
		// When a model already exists, title candidates added to akaModels must be
		// cleaned (uppercased and validated), not raw strings.
		Product product = buildBaseProduct();
		product.getOfferNames().add("Samsung HG32EJ690WE television");

		service.extractModelFromTitles(product);

		// The alternate must be the cleaned uppercased form
		assertThat(product.getAkaModels())
				.as("akaModels should contain cleaned uppercase HG32EJ690WE")
				.contains("HG32EJ690WE");
		// Canonical must not have changed
		assertThat(product.model()).isEqualTo("ModelX");
	}

	private void addSourcedAttribute(Product product, String name, String value, String datasourceName) {
		ProductAttribute attribute = new ProductAttribute();
		attribute.setName(name);
		attribute.addSourceAttribute(new org.open4goods.model.attribute.SourcedAttribute(new Attribute(name, value), datasourceName));
		product.getAttributes().getAll().put(name, attribute);
	}
}
