

package org.open4goods.ui.controllers.api;

import java.io.IOException;
import java.util.AbstractMap.SimpleEntry;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.open4goods.dao.ProductRepository;
import org.open4goods.exceptions.ResourceNotFoundException;
import org.open4goods.model.BarcodeType;
import org.open4goods.model.product.Product;
import org.open4goods.services.BarcodeValidationService;
import org.open4goods.services.VerticalsConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * This controller allows informations and communications about DatasourceConfigurations
 * @author goulven
 *
 */
@RestController

public class ApiProductController {

	@Autowired
	private  ProductRepository productRepository;
	
	@Autowired
	private  VerticalsConfigService configService;
		

	private @Autowired BarcodeValidationService barcodeValidationService;

	

	@GetMapping(path="/webextension/exists")
	public Entry<String,Boolean> webExtensionProduct(@RequestParam(required = false) String gtin,@RequestParam(required = false) String title , final HttpServletRequest request, HttpServletResponse response) throws IOException {


		// Checking through GTIN
		if (null != gtin) {
			
			SimpleEntry<BarcodeType, String> bCode = barcodeValidationService.sanitize(gtin.toString());
			
			if (bCode.getKey().equals(BarcodeType.UNKNOWN)) {
				return Map.entry("exists", false);
//				throw new ResponseStatusException(HttpStatus.BAD_REQUEST,	"Invalid GTIN Format");
			} else {
				Product data;
				try {
					data = productRepository.getById(bCode.getValue());
				} catch (ResourceNotFoundException e) {
					return Map.entry("exists", false);
				}
				if (null != data) {
                    return Map.entry("exists", true);
				} else {
					return Map.entry("exists", false);
				}
			}
			
		} else {
			// Lookup by title
			
			List<Product> data = productRepository.getByTitle(title);
			if (data.size() == 0) {
				return Map.entry("exists", false);
			} else {
                return Map.entry("exists", true);
			}
		}
		
		
		
	}

}
