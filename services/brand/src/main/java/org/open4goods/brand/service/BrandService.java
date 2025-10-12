package org.open4goods.brand.service;

import java.io.InputStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.open4goods.brand.model.Brand;
import org.open4goods.services.remotefilecaching.service.RemoteFileCachingService;
import org.open4goods.services.serialisation.service.SerialisationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;

/**
 * Service responsible for resolving brands and associating them with companies.
 */
public class BrandService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BrandService.class);

    private final RemoteFileCachingService remoteFileCachingService;
    private final SerialisationService serialisationService;

    private final Map<String, Long> missCounter = new ConcurrentHashMap<>();
    private final Map<String, Brand> brandsByName = new HashMap<>();

    public BrandService(RemoteFileCachingService remoteFileCachingService, SerialisationService serialisationService)
            throws Exception {
        this.remoteFileCachingService = remoteFileCachingService;
        this.serialisationService = serialisationService;
        loadBrandMappings();
    }

    private void loadBrandMappings() throws Exception {
        try {
            String mappingUrl =
                    "https://raw.githubusercontent.com/open4good/brands-company-mapping/refs/heads/main/brands-company-mapping.json";
            String mappingsStr = IOUtils.toString(new URL(mappingUrl), Charset.defaultCharset());

            Map<String, String> mappings = serialisationService.fromJson(mappingsStr,
                    new TypeReference<HashMap<String, String>>() {
                    });

            mappings.forEach((key, value) -> {
                Brand brand = new Brand(key);
                brand.setCompanyName(value);
                brandsByName.put(key, brand);
            });
        } catch (Exception e) {
            LOGGER.error("Error while loading brand mappings", e);
            throw e;
        }
    }

    public Brand resolve(String brandName) {
        String input = sanitizeBrand(brandName);
        LOGGER.info("Resolving brand {} ({})", brandName, input);

        Brand resolved = brandsByName.get(input);
        if (resolved == null) {
            resolved = new Brand(input);
            LOGGER.info("Brand not found in companies mapping : {}", resolved);
        } else {
            LOGGER.info("Brand found in companies mapping : {}", resolved);
        }
        return resolved;
    }

    public String sanitizeBrand(String name) {
        if (StringUtils.isEmpty(name)) {
            return "";
        }
        return StringUtils.stripAccents(name.toUpperCase()).trim();
    }

    public void incrementUnknown(String brand) {
        missCounter.merge(brand, 1L, Long::sum);
    }

    public boolean hasLogo(String upperCase) {
        return false;
    }

    public InputStream getLogo(String upperCase) {
        return null;
    }

    public Map<String, Long> getMissCounter() {
        return missCounter;
    }
}
