package org.open4goods.api.services.aggregation.services.realtime;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.open4goods.brand.service.BrandService;
import org.open4goods.icecat.services.IcecatService;
import org.open4goods.model.attribute.ProductAttribute;
import org.open4goods.model.attribute.ReferentielKey;
import org.open4goods.model.eprel.EprelProduct;
import org.open4goods.model.product.Product;
import org.open4goods.model.vertical.AttributeConfig;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.verticals.VerticalsConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class AttributeRealtimeAggregationServiceTest {

        private AttributeRealtimeAggregationService service;
        private VerticalConfig verticalConfig;

        @BeforeEach
        void setUp() {
                Logger logger = LoggerFactory.getLogger(AttributeRealtimeAggregationServiceTest.class);
                BrandService brandService = Mockito.mock(BrandService.class);
                VerticalsConfigService verticalConfigService = Mockito.mock(VerticalsConfigService.class);
                IcecatService icecatService = Mockito.mock(IcecatService.class);
                service = new AttributeRealtimeAggregationService(verticalConfigService, brandService, logger, icecatService);

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

        private Product buildBaseProduct() {
                Product product = new Product(123L);
                product.getAttributes().getReferentielAttributes().put(ReferentielKey.BRAND, "Brand");
                product.getAttributes().getReferentielAttributes().put(ReferentielKey.MODEL, "ModelX");
                product.setEprelDatas(new EprelProduct());
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
}
