package org.open4goods.b2bapi.service;

import java.util.List;
import org.open4goods.b2bapi.dto.product.B2bEnergyDto;
import org.open4goods.b2bapi.dto.product.B2bEprelAttributionDto;
import org.open4goods.model.eprel.EprelProduct;
import org.open4goods.model.product.Product;
import org.springframework.stereotype.Service;

/**
 * Maps the EPREL-sourced energy label data of a product to the sanitized B2B energy facet DTO.
 * <p>
 * Only an allow-listed subset of {@link EprelProduct} is exposed: supplier/organisation contact
 * details (address, phone, email) and the heavy {@code categorySpecificAttributes} payload are
 * never mapped, per the b2b-api "sanitized DTOs only" convention.
 */
@Service
public class ProductEprelMappingService {

    private static final String ATTRIBUTION_SOURCE = "EPREL - European Product Registry for Energy Labelling";
    private static final String ATTRIBUTION_URL = "https://eprel.ec.europa.eu";
    private static final String ATTRIBUTION_NOTICE =
            "Data sourced from the EU EPREL database. Free of charge for any authenticated account; never billed.";

    /**
     * Maps a product's EPREL data to the public energy facet payload.
     *
     * @param product product aggregate from Elasticsearch
     * @param gtin normalized request GTIN to echo in the response
     * @return sanitized energy facet DTO, or {@code null} when the product carries no EPREL data
     */
    public B2bEnergyDto map(final Product product, final String gtin) {
        final EprelProduct eprel = product == null ? null : product.getEprelDatas();
        if (eprel == null) {
            return null;
        }

        return new B2bEnergyDto(
                gtin,
                eprel.getEnergyClass(),
                eprel.getEnergyClassImage(),
                eprel.getProductGroup(),
                eprel.getEprelCategories() == null ? List.of() : List.copyOf(eprel.getEprelCategories()),
                eprel.getModelIdentifier(),
                eprel.getSupplierOrTrademark(),
                eprel.getEprelRegistrationNumber(),
                new B2bEprelAttributionDto(ATTRIBUTION_SOURCE, ATTRIBUTION_URL, ATTRIBUTION_NOTICE));
    }
}
