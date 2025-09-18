package org.open4goods.b2b.service;

import java.util.Locale;
import java.util.Set;

import org.open4goods.b2b.dto.product.ProductDto;
import org.open4goods.b2b.dto.product.RequestMetadata;
import org.open4goods.b2b.service.facets.PriceFacetService;
import org.open4goods.model.exceptions.ResourceNotFoundException;
import org.open4goods.model.product.Product;
import org.open4goods.services.productrepository.services.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import model.AvailableFacets;

/**
 * this is the top level mapping service, that will :
 * - Automatically instanciate facets converters according to enum @see {@link AvailableFacets}
 * - Defer each data transformation to it's dedicated facet converter
 */

@Service
public class ProductAccessService {

    private static final Logger logger = LoggerFactory.getLogger(ProductAccessService.class);

    private final ProductRepository repository;

	private PriceFacetService priceFacetService;

    public ProductAccessService(ProductRepository repository) {
    	this.repository = repository;

    	// Loading availlable facets
    	// NOTE : Complete here when setting a new facet
    	this.priceFacetService = new PriceFacetService();
    }

    /**
     *
     * @param gtin
     * @param local
     * @param includedFacets
     * @return
     */
    public ProductDto getProduct(long gtin, Locale local, Set<AvailableFacets> includedFacets) throws ResourceNotFoundException {
        Product p = repository.getById(gtin);
        if (null == p) {
        	throw new ResourceNotFoundException();
        }

        ProductDto pdto = mapProduct(p, local, includedFacets);
        return pdto;
    }

	private ProductDto mapProduct(  Product p, Locale local, Set<AvailableFacets> includedFacets) {

		ProductDto productDto = new ProductDto(p.getId());

		RequestMetadata metadatas = new RequestMetadata();
		metadatas.setTimestampEpoch(System.currentTimeMillis());

		if (null != includedFacets) {
		    for (AvailableFacets requiredFacet : includedFacets) {
		        switch (requiredFacet) {
		            case Price:
		                productDto.setPriceFacet(priceFacetService.render(p, local));
		                // Setting the facet cost
		                metadatas.getFacetsCosts().put(requiredFacet, priceFacetService.getCreditsCost());
		                break;
		            // NOTE : Complete here when new facets
		            default:
		                logger.error("Facet {} does not exist", requiredFacet);
		                break;
		        }
		    }
		}

		// computing total cost
		metadatas.setRequestCost(metadatas.getFacetsCosts().values().stream().mapToInt(Short::intValue).sum());
		// Computing duration
		metadatas.setDurationMs(System.currentTimeMillis() - metadatas.getTimestampEpoch());

		// Setting metadatas
		productDto.setRequestMetadatas(metadatas);

		return productDto;
	}



}
