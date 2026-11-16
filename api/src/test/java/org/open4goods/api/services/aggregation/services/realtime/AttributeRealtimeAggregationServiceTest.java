package org.open4goods.api.services.aggregation.services.realtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.open4goods.api.services.completion.EprelCompletionService;
import org.open4goods.brand.model.Brand;
import org.open4goods.brand.service.BrandService;
import org.open4goods.icecat.services.IcecatFeatureResolver;
import org.open4goods.icecat.util.IcecatConstants;
import org.open4goods.model.attribute.Attribute;
import org.open4goods.model.attribute.AttributeType;
import org.open4goods.model.attribute.IndexedAttribute;
import org.open4goods.model.attribute.ProductAttribute;
import org.open4goods.model.attribute.ReferentielKey;
import org.open4goods.model.attribute.SourcedAttribute;
import org.open4goods.model.datafragment.DataFragment;
import org.open4goods.model.eprel.EprelProduct;
import org.open4goods.model.exceptions.ValidationException;
import org.open4goods.model.product.Product;
import org.open4goods.model.vertical.AttributeConfig;
import org.open4goods.model.vertical.AttributesConfig;
import org.open4goods.model.vertical.ImpactScoreV2Config;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.services.serialisation.service.SerialisationService;
import org.open4goods.verticals.GoogleTaxonomyService;
import org.open4goods.verticals.VerticalsConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

class AttributeRealtimeAggregationServiceTest {

        /** Verticals and attributes as shipped in the {@code verticals} module. */
        private static VerticalsConfigService realVerticals;

        private AttributeRealtimeAggregationService service;
        private VerticalConfig verticalConfig;
        private IcecatFeatureResolver icecatFeatureResolver;
        private BrandService brandService;

        @BeforeAll
        static void loadRealVerticals() {
                realVerticals = new VerticalsConfigService(new SerialisationService(),
                                Mockito.mock(GoogleTaxonomyService.class), new PathMatchingResourcePatternResolver());
        }

        @BeforeEach
        void setUp() {
                Logger logger = LoggerFactory.getLogger(AttributeRealtimeAggregationServiceTest.class);
                brandService = Mockito.mock(BrandService.class);
                Mockito.when(brandService.resolve(Mockito.anyString(), Mockito.anyMap()))
                                .thenAnswer(invocation -> new Brand(invocation.getArgument(0, String.class).toUpperCase()));
                icecatFeatureResolver = Mockito.mock(IcecatFeatureResolver.class);
                service = new AttributeRealtimeAggregationService(brandService, logger, icecatFeatureResolver);

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

        /**
         * GOU-1841: "Overriding GTIN from 9781609202415 to 1609202414" replaced the
         * sanitised ISBN-13 by the raw ISBN-10 of the fragment.
         */
        @Test
        void handleReferentielAttributesKeepsTheSanitisedGtin() throws Exception {
                Product product = new Product(9781609202415L);
                product.getAttributes().getReferentielAttributes().put(ReferentielKey.GTIN, "9781609202415");
                DataFragment fragment = new DataFragment();
                fragment.getReferentielAttributes().put(ReferentielKey.GTIN, "1609202414");

                Method method = AttributeRealtimeAggregationService.class.getDeclaredMethod("handleReferentielAttributes", DataFragment.class, Product.class, VerticalConfig.class);
                method.setAccessible(true);
                method.invoke(service, fragment, product, verticalConfig);

                assertThat(product.gtin()).isEqualTo("9781609202415");
                assertThat(product.getGtinInfos().getGtinStrings()).containsExactly("1609202414");
        }

        @Test
        void onProductAssignsIcecatTaxonomyIdsFromResolver() throws Exception {
                Product product = buildBaseProduct();
                addAttribute(product, "COULEUR", IcecatConstants.DATASOURCE_NAME);
                Mockito.when(icecatFeatureResolver.resolveFeatureName("COULEUR")).thenReturn(Set.of(46, 46757));

                service.onProduct(product, verticalConfig);

                assertThat(product.getAttributes().getAll().get("COULEUR").getIcecatTaxonomyIds())
                                .containsExactlyInAnyOrder(46, 46757);
        }

        @Test
        void onProductDoesNotAskIcecatAboutAttributesIcecatNeverAsserted() throws Exception {
                Product product = buildBaseProduct();
                addAttribute(product, "COULEUR", EprelCompletionService.EPREL_DS_NAME);
                addAttribute(product, "LARGEUR", "some-merchant.com");

                service.onProduct(product, verticalConfig);

                assertThat(product.getAttributes().getAll().get("COULEUR").getIcecatTaxonomyIds()).isEmpty();
                assertThat(product.getAttributes().getAll().get("LARGEUR").getIcecatTaxonomyIds()).isEmpty();
                Mockito.verify(icecatFeatureResolver, Mockito.never()).resolveFeatureName(Mockito.anyString());
        }

        @Test
        void onProductClearsIcecatTaxonomyIdsLeftOnANonIcecatAttribute() throws Exception {
                Product product = buildBaseProduct();
                ProductAttribute corrupted = addAttribute(product, "COULEUR", EprelCompletionService.EPREL_DS_NAME);
                corrupted.setIcecatTaxonomyIds(new HashSet<>(Set.of(46)));

                service.onProduct(product, verticalConfig);

                assertThat(product.getAttributes().getAll().get("COULEUR").getIcecatTaxonomyIds()).isEmpty();
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

        @Test
        void onProductElectsTheEprelBrand_throughTheNormalBrandPath() throws Exception {
                Product product = new Product(123L);
                EprelProduct eprelProduct = new EprelProduct();
                eprelProduct.setSupplierOrTrademark("Brand");
                eprelProduct.setModelIdentifier("ModelX");
                product.setEprelDatas(eprelProduct);
                product.addBrand("first-merchant.com", "First brand", null, null);
                product.addBrand("some-merchant.com", "Other brand", null, null);

                service.onProduct(product, verticalConfig);

                // EPREL is the trusted brand evidence, and the merchant claims stay
                // recorded against the datasource that made them.
                assertThat(product.brand()).isEqualTo("BRAND");
                assertThat(product.getAkaBrands()).containsEntry("some-merchant.com", "OTHER BRAND");
        }

        @Test
        void onProductDoesNotReAssertADerivedBrand_whenEprelClaimsTheReferentielSlot() throws Exception {
                Product product = new Product(123L);
                EprelProduct eprelProduct = new EprelProduct();
                eprelProduct.setSupplierOrTrademark("Brand");
                product.setEprelDatas(eprelProduct);
                // A single merchant claim lands straight in the referentiel slot without being
                // keyed by datasource, so after election it is a derived value, not evidence.
                product.addBrand("only-merchant.com", "Other brand", null, null);

                service.onProduct(product, verticalConfig);

                assertThat(product.brand()).isEqualTo("BRAND");
                // It is not re-filed as an alternate brand: it has no datasource key to be
                // filed under, and the merchant re-asserts it on its next fragment.
                assertThat(product.getAkaBrands()).isEmpty();
        }

        @Test
        void onProductDoesNotWriteAnEprelBrandThatTheVerticalExcludes() throws Exception {
                Product product = new Product(123L);
                EprelProduct eprelProduct = new EprelProduct();
                eprelProduct.setSupplierOrTrademark("Excluded");
                product.setEprelDatas(eprelProduct);
                verticalConfig.setBrandsExclusion(Set.of("EXCLUDED"));

                service.onProduct(product, verticalConfig);

                assertThat(product.brand()).isNullOrEmpty();
        }

        @Test
        void onProductElectsTheEprelModel_ratherThanWritingItDirectly() throws Exception {
                Product product = new Product(123L);
                EprelProduct eprelProduct = new EprelProduct();
                eprelProduct.setModelIdentifier("model-x/2024");
                product.setEprelDatas(eprelProduct);

                service.onProduct(product, verticalConfig);

                // The raw EPREL identifier is never stored as-is: it goes through the same
                // cleaning and canonical election as any other model candidate.
                assertThat(product.model()).isNotEqualTo("model-x/2024");
                assertThat(product.model()).isEqualTo(product.model() == null ? null : product.model().toUpperCase());
        }

        /**
         * GOU-1881: the batch runs updateExcludeStatus on every product of the vertical, so a product excluded for
         * missing_eprel is re-evaluated: listed once the vertical goes V2 with eprelRequired=false and its coverage
         * reaches the threshold (GOU-2165), excluded again on return to V1.
         */
        @Test
        void updateExcludeStatusReevaluatesExcludedProductsWhenEprelRequirementChanges() throws Exception {
                Product product = buildBaseProduct();
                addAttribute(product, "required_attr");
                product.setEprelDatas(null);

                invokeUpdateExcludeStatus(product);
                assertThat(product.getExcludedCauses()).containsExactly("missing_eprel");

                AttributeConfig criterion = new AttributeConfig();
                criterion.setKey("NOISE");
                criterion.setAsScore(true);
                criterion.setFilteringType(AttributeType.NUMERIC);
                AttributesConfig attributesConfig = new AttributesConfig();
                attributesConfig.setConfigs(List.of(criterion));
                verticalConfig.setAttributesConfig(attributesConfig);
                ImpactScoreV2Config v2 = new ImpactScoreV2Config();
                v2.setEprelRequired(false);
                v2.setMinRealWeightCoverage(0.5);
                v2.setCriteriasPonderation(Map.of("NOISE", 1.0));
                verticalConfig.getImpactScoreConfig().setV2(v2);
                verticalConfig.getImpactScoreConfig().setMethodologyVersion(2);
                invokeUpdateExcludeStatus(product);
                assertThat(product.getExcludedCauses()).containsExactly("missing_eprel_low_coverage");

                product.getAttributes().getIndexed().put("NOISE", new IndexedAttribute("NOISE", "42"));
                invokeUpdateExcludeStatus(product);
                assertThat(product.isExcluded()).isFalse();
                assertThat(product.getExcludedCauses()).isEmpty();

                verticalConfig.getImpactScoreConfig().setMethodologyVersion(1);
                invokeUpdateExcludeStatus(product);
                assertThat(product.isExcluded()).isTrue();
                assertThat(product.getExcludedCauses()).containsExactly("missing_eprel");
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
                addAttribute(product, name, null);
        }

        /**
         * Adds a raw attribute contributed by {@code datasource}, or with no source at
         * all when {@code datasource} is {@code null}.
         */
        private ProductAttribute addAttribute(Product product, String name, String datasource) {
                ProductAttribute attribute = new ProductAttribute();
                attribute.setName(name);
                attribute.setValue("value");
                if (datasource != null) {
                        SourcedAttribute source = new SourcedAttribute();
                        source.setDataSourcename(datasource);
                        source.setName(name);
                        source.setValue("value");
                        attribute.addSourceAttribute(source);
                }
                product.getAttributes().getAll().put(name, attribute);
                return attribute;
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
		
		IndexedAttribute indexedAttr = new IndexedAttribute("ENERGY_CLASS", "F");

		// Act
		Method method = AttributeRealtimeAggregationService.class.getDeclaredMethod("mergeSourcesAndRefreshValue", IndexedAttribute.class, ProductAttribute.class, AttributeConfig.class, VerticalConfig.class);
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
	void mergeSourcesKeepsATextValueThatOnlyLooksNumeric() throws Exception {
		// IdHelper.isPureDouble accepts ".", which Double.valueOf rejects: the text value is kept without a
		// numeric value instead of failing the attribute.
		AttributeConfig attrConfig = new AttributeConfig();
		attrConfig.setKey("COLOR");

		ProductAttribute attr = new ProductAttribute();
		attr.setName("COLOR");
		SourcedAttribute source = new SourcedAttribute();
		source.setDataSourcename("source1");
		source.setValue(".");
		source.setName("COLOR");
		attr.addSourceAttribute(source);

		IndexedAttribute indexedAttr = new IndexedAttribute("COLOR", ".");

		Method method = AttributeRealtimeAggregationService.class.getDeclaredMethod("mergeSourcesAndRefreshValue",
				IndexedAttribute.class, ProductAttribute.class, AttributeConfig.class, VerticalConfig.class);
		method.setAccessible(true);
		method.invoke(service, indexedAttr, attr, attrConfig, verticalConfig);

		assertThat(indexedAttr.getValue()).isEqualTo(".");
		assertThat(indexedAttr.getNumericValue()).isNull();
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

	/**
	 * GOU-1897 point 3: the "W" token was stripped after upper-casing, so "1,2 KW" read 1.2 W and "1 200 W" read 1 W.
	 */
	@ParameterizedTest
	@CsvSource(delimiter = '|', value = {
			"1,2 KW|1200", "1.2kW|1200", "1 200 W|1200", "1\u00a0200 W|1200", "1200 W|1200", "2,5 kW|2500",
			"0,3 W|0.3", "0, 3 W|0.3", "0.5 W|0.5", "500 mW|0.5", "1,2 kilowatts|1200", "900 Watts|900", "1500|1500",
			// GOU-2315: an energy per hour is a power
			"0,85 kWh/h|850", "0.85 KWH/H|850", "1,1 kWh/60 min|1100", "2 kWh / heure|2000", "500 Wh/h|500" })
	void powerIsConvertedToWatts(String raw, String expected) throws Exception {
		AttributeConfig attrConfig = wattAttribute();

		assertThat(service.parseValue(raw, attrConfig, verticalConfig)).isEqualTo(expected);
	}

	/**
	 * GOU-1892: merchant formats of the French durability index all read as a plain 0-10 number.
	 */
	@ParameterizedTest
	@CsvSource(delimiter = '|', value = { "7,2/10|7.2", "7.2|7.2", "7,2 sur 10|7.2", "7,2 / 10|7.2", "8/10|8", "6,5 SUR 10|6.5" })
	void durabilityIndexFormatsAreParsed(String raw, String expected) throws Exception {
		AttributeConfig attrConfig = new AttributeConfig();
		attrConfig.setKey("DURABILITY_INDEX");
		attrConfig.setFilteringType(AttributeType.NUMERIC);
		attrConfig.getParser().setUpperCase(true);
		attrConfig.getParser().setNormalize(true);
		attrConfig.getParser().setTrim(true);
		attrConfig.getParser().setDeleteTokens(java.util.List.of("/10", "/ 10", "SUR 10"));

		assertThat(service.parseValue(raw, attrConfig, verticalConfig)).isEqualTo(expected);
	}

	/**
	 * GOU-2315: an energy (per cycle, per year) is not a power; it used to lose its unit and read as watts.
	 */
	@ParameterizedTest
	@ValueSource(strings = { "0.543 kWh", "84 kWh/an", "52 kWh/100 cycles", "300 Wh" })
	void energyIsNotReadAsWatts(String raw) {
		AttributeConfig attrConfig = wattAttribute();

		assertThatThrownBy(() -> service.parseValue(raw, attrConfig, verticalConfig))
				.isInstanceOf(ValidationException.class);
	}

	private static AttributeConfig wattAttribute() {
		AttributeConfig attrConfig = new AttributeConfig();
		attrConfig.setKey("OVEN_POWER");
		attrConfig.setFilteringType(AttributeType.NUMERIC);
		org.open4goods.model.Localisable<String, String> suffix = new org.open4goods.model.Localisable<>();
		suffix.put("default", "W");
		attrConfig.setSuffix(suffix);
		attrConfig.getParser().setUpperCase(true);
		attrConfig.getParser().setNormalize(true);
		attrConfig.getParser().setTrim(true);
		attrConfig.getParser().setDeleteTokens(java.util.List.of("W", "WATT", "WATTS"));
		return attrConfig;
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
		attrConfig.setFilteringType(AttributeType.NUMERIC);
		attrConfig.getParser().setUpperCase(true);
		attrConfig.getParser().setNormalize(true);
		attrConfig.getParser().setTrim(true);

		assertThat(service.parseValue("false", attrConfig, verticalConfig)).isEmpty();
	}

	@ParameterizedTest
	@ValueSource(strings = { "Donnée non spécifiée", "Données non spécifiées", "Non spécifié", "Non spécifiée", "N/A",
			"NA", "nc", "null", "FALSE" })
	void parseValueTreatsPlaceholdersAsEmpty(String raw) throws Exception {
		AttributeConfig attrConfig = new AttributeConfig();
		attrConfig.setKey("COLOR");

		assertThat(service.parseValue(raw, attrConfig, verticalConfig)).isEmpty();
	}

	@Test
	void parseValueKeepsTheWarrantyOnlyPlaceholder() throws Exception {
		AttributeConfig attrConfig = new AttributeConfig();
		attrConfig.setKey("COLOR");

		// "eur" is a placeholder for WarrantyParser only: sharing the common set must not bring it here.
		assertThat(service.parseValue("eur", attrConfig, verticalConfig)).isNotEmpty();
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

	/**
	 * GOU-2276: the numeric extraction stopped at the first space, so "1 400 tr/min" read 1 for SPIN_SPEED.
	 */
	@ParameterizedTest
	@CsvSource(delimiter = '|', value = { "1 400 tr/min|1400", "1\u00a0400 tr/min|1400", "1\u202f400 TR/MIN|1400",
			"1\u2009400 tr/min|1400", "1400 tr/min|1400", "1 200,5|1200.5", "12 345 678|12345678", "2 088|2088",
			"7,2|7.2", "12 34|12", "1400 120|1400" })
	void numericValuesKeepSpaceGroupedThousands(String raw, String expected) throws Exception {
		AttributeConfig spinSpeed = realAttribute("washing-machine", "SPIN_SPEED");

		assertThat(service.parseValue(raw, spinSpeed, verticalConfig)).isEqualTo(expected);
	}

	/**
	 * GOU-2276: thousands are only joined across a space of the received value. A space left behind by a removed
	 * quote mark or unit token never joins two numbers: "55\" 140 cm" must not read 55140.
	 */
	@ParameterizedTest
	@CsvSource(delimiter = '|', value = { "tv|DIAGONALE_POUCES|55\" 140 cm|55",
			"tv|DIAGONALE_POUCES|55\u2019\u2019 139 cm|55", "tv|DIAGONALE_POUCES|6\" 152 mm|6",
			"washing-machine|ENERGY_CONSUMPTION_100_CYCLES|52 kWh 100 cycles|52",
			"washing-machine|SPIN_SPEED|1 400 tr/min|1400" })
	void thousandsAreNotJoinedAcrossRemovedTokens(String vertical, String key, String raw, String expected)
			throws Exception {
		AttributeConfig attribute = realAttribute(vertical, key);

		assertThat(service.parseValue(raw, attribute, verticalConfig)).isEqualTo(expected);
	}

	@Test
	void onProductIndexesTheSpinSpeedWithItsThousands() throws Exception {
		verticalConfig.getAttributesConfig().setConfigs(List.of(realAttribute("washing-machine", "SPIN_SPEED")));
		Product product = new Product(123L);
		addSourcedAttribute(product, "VITESSE D'ESSORAGE", "1 400 tr/min", "some-merchant.com");

		service.onProduct(product, verticalConfig);

		IndexedAttribute spinSpeed = product.getAttributes().getIndexed().get("SPIN_SPEED");
		assertThat(spinSpeed.getValue()).isEqualTo("1400");
		assertThat(spinSpeed.getNumericValue()).isEqualTo(1400.0);
	}

	/**
	 * GOU-2315: EPREL sends the hourly consumption of an air conditioner without a unit, in kWh/h; the shipped
	 * {@code sourceUnits} reads it in watts. A unitless Icecat value of the same attribute stays in watts.
	 */
	@Test
	void onProductReadsTheUnitlessEprelHourlyConsumptionInKilowatts() throws Exception {
		verticalConfig.getAttributesConfig()
				.setConfigs(List.of(realAttribute("air-conditioner", "POWER_CONSUMPTION_TYPICAL")));
		Product eprel = new Product(123L);
		addSourcedAttribute(eprel, "coolingCharacteristics-hourlyElectricityConsumption", "1.1",
				EprelCompletionService.EPREL_DS_NAME);
		Product icecat = new Product(124L);
		addSourcedAttribute(icecat, "CONSOMMATION ELECTRIQUE TYPIQUE", "950", "icecat.biz");

		service.onProduct(eprel, verticalConfig);
		service.onProduct(icecat, verticalConfig);

		IndexedAttribute fromEprel = eprel.getAttributes().getIndexed().get("POWER_CONSUMPTION_TYPICAL");
		assertThat(fromEprel.getValue()).isEqualTo("1100");
		assertThat(fromEprel.getNumericValue()).isEqualTo(1100.0);
		assertThat(fromEprel.getSource()).extracting(SourcedAttribute::getCleanedValue).containsExactly("1100");
		assertThat(icecat.getAttributes().getIndexed().get("POWER_CONSUMPTION_TYPICAL").getValue()).isEqualTo("950");
	}

	/**
	 * GOU-2315: a source that no longer parses (an energy in kWh) does not vote with the cleaned value left by an
	 * earlier aggregation, even when its datasource ranks first.
	 */
	@Test
	void onProductDropsTheStaleCleanedValueOfASourceThatNoLongerParses() throws Exception {
		verticalConfig.getAttributesConfig()
				.setConfigs(List.of(realAttribute("air-conditioner", "POWER_CONSUMPTION_TYPICAL")));
		Product product = new Product(123L);
		addSourcedAttribute(product, "CONSOMMATION ELECTRIQUE TYPIQUE", "950 W", "icecat.biz");
		SourcedAttribute energy = new SourcedAttribute(new Attribute("CONSOMMATION ELECTRIQUE TYPIQUE", "0,5 kWh"),
				EprelCompletionService.EPREL_DS_NAME);
		energy.setCleanedValue("0.5");
		product.getAttributes().getAll().get("CONSOMMATION ELECTRIQUE TYPIQUE").addSourceAttribute(energy);

		service.onProduct(product, verticalConfig);

		assertThat(product.getAttributes().getIndexed().get("POWER_CONSUMPTION_TYPICAL").getValue()).isEqualTo("950");
	}

	/**
	 * GOU-2276: the empty-placeholder test ran before the mappings and its list holds "false", so the
	 * {@code "FALSE": "NON"} mapping of BATTERY_USER_REPLACEABLE never applied and only "OUI" was indexed.
	 */
	@ParameterizedTest
	@CsvSource(delimiter = '|', value = { "false|NON", "true|OUI" })
	void onProductMapsTheEprelBooleanOfBatteryUserReplaceable(String eprelValue, String expected) throws Exception {
		verticalConfig.getAttributesConfig()
				.setConfigs(List.of(realAttribute("smartphones", "BATTERY_USER_REPLACEABLE")));
		Product product = new Product(123L);
		addSourcedAttribute(product, "BATTERYUSERREPLACEABLE", eprelValue, EprelCompletionService.EPREL_DS_NAME);

		service.onProduct(product, verticalConfig);

		assertThat(product.getAttributes().getIndexed().get("BATTERY_USER_REPLACEABLE").getValue())
				.isEqualTo(expected);
	}

	/**
	 * GOU-2276: with a custom parser, the value of a key fed by two raw attributes was the parse of whichever raw
	 * attribute the HashMap returned last, instead of an election over all the sources merged under the key.
	 */
	@Test
	void onProductElectsTheSameWeightWhicheverRawAttributeComesFirst() throws Exception {
		verticalConfig.getAttributesConfig().setConfigs(List.of(realAttribute("washing-machine", "WEIGHT")));

		Product merchantFirst = productWithAttributesInOrder(
				new String[] { "POIDS", "72 kg", "some-merchant.com" },
				new String[] { "WEIGHT", "70.5 kg", IcecatConstants.DATASOURCE_NAME });
		Product icecatFirst = productWithAttributesInOrder(
				new String[] { "WEIGHT", "70.5 kg", IcecatConstants.DATASOURCE_NAME },
				new String[] { "POIDS", "72 kg", "some-merchant.com" });

		service.onProduct(merchantFirst, verticalConfig);
		service.onProduct(icecatFirst, verticalConfig);

		IndexedAttribute weightMerchantFirst = merchantFirst.getAttributes().getIndexed().get("WEIGHT");
		IndexedAttribute weightIcecatFirst = icecatFirst.getAttributes().getIndexed().get("WEIGHT");
		assertThat(weightMerchantFirst.sourcesCount()).isEqualTo(2);
		assertThat(weightIcecatFirst.sourcesCount()).isEqualTo(2);
		assertThat(weightMerchantFirst.getValue()).isEqualTo(weightIcecatFirst.getValue());
		assertThat(weightMerchantFirst.getNumericValue()).isEqualTo(weightIcecatFirst.getNumericValue());
		// Both readings are equally confident: the trusted Icecat source is offered first and wins the tie
		assertThat(weightMerchantFirst.getNumericValue()).isEqualTo(70.5);
	}

	/**
	 * GOU-2276: {@code Product.akaBrands()} returns a filtered copy, so clearing it left the stored alternate brands
	 * untouched and a brand excluded after ingestion stayed in akaBrands.
	 */
	@Test
	void onProductDropsANewlyExcludedBrandFromAkaBrands() throws Exception {
		Product product = new Product(123L);
		product.addBrand("first-merchant.com", "Brand", null, null);
		product.addBrand("old-merchant.com", "Old brand", null, null);
		product.addBrand("other-merchant.com", "Other brand", null, null);
		assertThat(product.getAkaBrands()).containsEntry("old-merchant.com", "OLD BRAND");
		verticalConfig.setBrandsExclusion(Set.of("OLD BRAND"));

		service.onProduct(product, verticalConfig);

		assertThat(product.brand()).isEqualTo("BRAND");
		assertThat(product.getAkaBrands()).containsExactly(Map.entry("other-merchant.com", "OTHER BRAND"));
	}

	/**
	 * GOU-2295: three merchants vote X, one source votes Y. X wins by votes unless the attribute lists Y's source
	 * in {@code preferredSources}; without the field, eprel and icecat.biz keep their rank.
	 */
	@Test
	void onProductElectsTheValueOfAPreferredSourceOverTheVotes() throws Exception {
		Product byVotes = productWithRegulatorAndThreeMerchants();
		Product preferred = productWithRegulatorAndThreeMerchants();

		verticalConfig.getAttributesConfig().setConfigs(List.of(indexAttribute(null)));
		service.onProduct(byVotes, verticalConfig);
		verticalConfig.getAttributesConfig().setConfigs(List.of(indexAttribute(List.of("regulator"))));
		service.onProduct(preferred, verticalConfig);

		assertThat(byVotes.getAttributes().getIndexed().get("INDEX").getValue()).isEqualTo("5");
		assertThat(preferred.getAttributes().getIndexed().get("INDEX").getValue()).isEqualTo("8");
	}

	@Test
	void onProductKeepsEprelFirstWithoutPreferredSources() throws Exception {
		verticalConfig.getAttributesConfig().setConfigs(List.of(indexAttribute(null)));
		Product product = productWithRegulatorAndThreeMerchants();
		addSourcedAttribute(product, "D", "9", EprelCompletionService.EPREL_DS_NAME);

		service.onProduct(product, verticalConfig);

		assertThat(product.getAttributes().getIndexed().get("INDEX").getValue()).isEqualTo("9");
	}

	private static AttributeConfig indexAttribute(List<String> preferredSources) {
		AttributeConfig config = new AttributeConfig();
		config.setKey("INDEX");
		config.setSynonyms(Map.of("all", Set.of("A", "B", "C", "D")));
		config.setPreferredSources(preferredSources);
		return config;
	}

	private Product productWithRegulatorAndThreeMerchants() {
		return productWithAttributesInOrder(
				new String[] { "A", "5", "shop-a.com" },
				new String[] { "B", "5", "shop-b.com" },
				new String[] { "C", "5", "shop-c.com" },
				new String[] { "D", "8", "regulator" });
	}

	/**
	 * Builds a product whose raw attributes are visited in the given order, each one being
	 * {@code {name, value, datasource}}.
	 */
	private Product productWithAttributesInOrder(String[]... attributes) {
		Product product = new Product(123L);
		product.getAttributes().setAll(new LinkedHashMap<>());
		for (String[] attribute : attributes) {
			addSourcedAttribute(product, attribute[0], attribute[1], attribute[2]);
		}
		return product;
	}

	/**
	 * Returns the attribute configuration shipped for a vertical, with its real synonyms, parser and mappings.
	 */
	private static AttributeConfig realAttribute(String verticalId, String key) {
		VerticalConfig vertical = realVerticals.getConfigById(verticalId);
		AttributeConfig attribute = vertical.getAttributesConfig().getAttributeConfigByKey(key);
		assertThat(attribute).as("%s in %s", key, verticalId).isNotNull();
		return attribute;
	}

	private void addSourcedAttribute(Product product, String name, String value, String datasourceName) {
		ProductAttribute attribute = new ProductAttribute();
		attribute.setName(name);
		attribute.addSourceAttribute(new org.open4goods.model.attribute.SourcedAttribute(new Attribute(name, value), datasourceName));
		product.getAttributes().getAll().put(name, attribute);
	}
}
