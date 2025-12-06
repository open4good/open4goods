package org.open4goods.model.product;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.open4goods.model.attribute.IndexedAttribute;
import org.open4goods.model.attribute.ProductAttribute;
import org.open4goods.model.attribute.SourcedAttribute;
import org.open4goods.model.eprel.EprelProduct;

class ProductTest
{

        @Test
        void removeDatasourceDataCleansEprelPayload()
        {
                Product product = new Product(1L);
                product.setEprelDatas(new EprelProduct());
                product.getExternalIds().setEprel("eprel-id");
                product.getDatasourceCodes().put("eprel", 123L);

                product.getDatasourceCategories().add("eprel-category");
                product.getDatasourceCategories().add("other-category");
                product.getCategoriesByDatasources().put("eprel", "eprel-category");
                product.getCategoriesByDatasources().put("other", "other-category");

                ProductAttribute eprelOnly = new ProductAttribute();
                eprelOnly.setName("eprel-only");
                eprelOnly.setValue("value");
                eprelOnly.getSource().add(source("eprel", "value"));
                product.getAttributes().getAll().put("eprel-only", eprelOnly);

                IndexedAttribute mixedSource = new IndexedAttribute();
                mixedSource.setName("mixed");
                mixedSource.setValue("eprel-value");
                mixedSource.getSource().add(source("eprel", "eprel-value"));
                mixedSource.getSource().add(source("other", "other-value"));
                product.getAttributes().getIndexed().put("mixed", mixedSource);

                product.removeDatasourceData("eprel");

                assertNull(product.getExternalIds().getEprel());
                assertNull(product.getEprelDatas());
                assertFalse(product.getDatasourceCodes().containsKey("eprel"));
                assertFalse(product.getCategoriesByDatasources().containsKey("eprel"));
                assertFalse(product.getDatasourceCategories().contains("eprel-category"));
                assertTrue(product.getDatasourceCategories().contains("other-category"));

                assertFalse(product.getAttributes().getAll().containsKey("eprel-only"));
                assertTrue(product.getAttributes().getIndexed().containsKey("mixed"));

                IndexedAttribute remainingAttribute = product.getAttributes().getIndexed().get("mixed");
                assertEquals("other-value", remainingAttribute.getValue());
                assertTrue(remainingAttribute.getSource().stream()
                                .allMatch(source -> "other".equalsIgnoreCase(source.getDataSourcename())));
        }

        private SourcedAttribute source(String datasource, String value)
        {
                SourcedAttribute sourcedAttribute = new SourcedAttribute();
                sourcedAttribute.setDataSourcename(datasource);
                sourcedAttribute.setValue(value);
                return sourcedAttribute;
        }
}
