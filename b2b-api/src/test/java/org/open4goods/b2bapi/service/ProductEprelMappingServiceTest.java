package org.open4goods.b2bapi.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.open4goods.b2bapi.dto.product.B2bEnergyDto;
import org.open4goods.model.eprel.EprelProduct;
import org.open4goods.model.product.Product;

/**
 * Unit tests for the EPREL energy facet mapping and its sanitization allow-list.
 */
class ProductEprelMappingServiceTest {

    private final ProductEprelMappingService service = new ProductEprelMappingService();

    @Test
    void returnsNullWhenProductHasNoEprelData() {
        final Product product = new Product(885909950805L);

        assertThat(service.map(product, "885909950805")).isNull();
        assertThat(service.map(null, "885909950805")).isNull();
    }

    @Test
    void mapsAllowListedFieldsAndIncludesMandatoryAttribution() {
        final Product product = new Product(885909950805L);
        final EprelProduct eprel = new EprelProduct();
        eprel.setEnergyClass("A");
        eprel.setEnergyClassImage("energy_class_A.png");
        eprel.setProductGroup("airconditioners");
        eprel.setEprelCategories(List.of("AIRCON_COMFORT"));
        eprel.setModelIdentifier("MODEL-123");
        eprel.setSupplierOrTrademark("Acme");
        eprel.setEprelRegistrationNumber("123456");
        product.setEprelDatas(eprel);

        final B2bEnergyDto dto = service.map(product, "0885909950805");

        assertThat(dto).isNotNull();
        assertThat(dto.gtin()).isEqualTo("0885909950805");
        assertThat(dto.energyClass()).isEqualTo("A");
        assertThat(dto.energyClassImage()).isEqualTo("energy_class_A.png");
        assertThat(dto.productGroup()).isEqualTo("airconditioners");
        assertThat(dto.eprelCategories()).containsExactly("AIRCON_COMFORT");
        assertThat(dto.modelIdentifier()).isEqualTo("MODEL-123");
        assertThat(dto.supplierOrTrademark()).isEqualTo("Acme");
        assertThat(dto.eprelRegistrationNumber()).isEqualTo("123456");
        assertThat(dto.attribution()).isNotNull();
        assertThat(dto.attribution().sourceUrl()).isEqualTo("https://eprel.ec.europa.eu");
        assertThat(dto.attribution().notice()).containsIgnoringCase("free of charge");
    }
}
