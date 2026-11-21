package org.open4goods.api.services.metriks.affiliation;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.open4goods.commons.config.yml.datasource.DataSourceProperties;
import org.open4goods.commons.services.DataSourceConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * {@link MerchantDatasourceResolver} built on the datasource configurations.
 * <p>
 * Awin: the programme identifier is the {@code awinmid} (or {@code mid}) query parameter of an {@code awin1.com}
 * link held by the datasource ({@code affiliationLinkPrefix}, {@code affiliatedPortalUrl}). A programme claimed by
 * two datasources is ambiguous and resolves to none. The other networks are not resolved yet: their merchants keep
 * their clicks "not measured".
 */
@Component
public class DatasourceMerchantResolver implements MerchantDatasourceResolver {

    private static final String AWIN = "awin";

    private final DataSourceConfigService dataSourceConfigService;
    private volatile Map<String, String> awinProgrammes;

    @Autowired
    public DatasourceMerchantResolver(DataSourceConfigService dataSourceConfigService) {
        this.dataSourceConfigService = dataSourceConfigService;
    }

    @Override
    public Optional<String> datasourceOf(String network, String advertiserId) {
        if (!AWIN.equals(network) || advertiserId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(awinProgrammes().get(advertiserId.trim()));
    }

    @Override
    public boolean hasProgramme(String network, String datasource) {
        return AWIN.equals(network) && datasource != null && awinProgrammes().containsValue(datasource);
    }

    private Map<String, String> awinProgrammes() {
        Map<String, String> map = awinProgrammes;
        if (map == null) {
            map = index(dataSourceConfigService.datasourceConfigs().values().stream().toList());
            awinProgrammes = map;
        }
        return map;
    }

    /**
     * @return datasource name per Awin programme id, ambiguous programmes excluded
     */
    static Map<String, String> index(Iterable<DataSourceProperties> datasources) {
        Map<String, String> byProgramme = new HashMap<>();
        Set<String> ambiguous = new HashSet<>();
        for (DataSourceProperties datasource : datasources) {
            if (!StringUtils.hasText(datasource.getName())) {
                continue;
            }
            Stream.of(datasource.getAffiliationLinkPrefix(), datasource.getAffiliatedPortalUrl())
                    .map(DatasourceMerchantResolver::awinProgrammeOf)
                    .flatMap(Optional::stream)
                    .distinct()
                    .forEach(programme -> {
                        String previous = byProgramme.putIfAbsent(programme, datasource.getName());
                        if (previous != null && !previous.equals(datasource.getName())) {
                            ambiguous.add(programme);
                        }
                    });
        }
        ambiguous.forEach(byProgramme::remove);
        return Map.copyOf(byProgramme);
    }

    /**
     * @return the {@code awinmid} / {@code mid} of an {@code awin1.com} link, empty for another link
     */
    static Optional<String> awinProgrammeOf(String link) {
        if (!StringUtils.hasText(link)) {
            return Optional.empty();
        }
        try {
            URI uri = URI.create(link.trim());
            String host = uri.getHost();
            if (host == null || !(host.equals("awin1.com") || host.endsWith(".awin1.com")) || uri.getRawQuery() == null) {
                return Optional.empty();
            }
            for (String pair : uri.getRawQuery().split("&")) {
                int eq = pair.indexOf('=');
                if (eq <= 0) {
                    continue;
                }
                String key = URLDecoder.decode(pair.substring(0, eq), StandardCharsets.UTF_8);
                String value = URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8).trim();
                if (("awinmid".equals(key) || "mid".equals(key)) && value.matches("\\d+")) {
                    return Optional.of(value);
                }
            }
        } catch (IllegalArgumentException e) {
            // Unreadable link: no programme.
        }
        return Optional.empty();
    }
}
