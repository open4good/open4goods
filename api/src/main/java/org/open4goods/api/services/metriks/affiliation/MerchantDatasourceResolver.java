package org.open4goods.api.services.metriks.affiliation;

import java.util.Optional;

/**
 * Maps an advertiser of an affiliation network to the datasource (the {@code merchant} of an
 * {@code AffiliationClick}), so that sales and clicks of one merchant sit on the same row of the Metriks merchant
 * report. The match is exact on the network programme identifier, never on a name.
 */
public interface MerchantDatasourceResolver {

    /**
     * @param network      network key ({@code awin}...)
     * @param advertiserId network identifier of the advertiser
     * @return the datasource name, empty when no configured datasource carries this programme
     */
    Optional<String> datasourceOf(String network, String advertiserId);

    /**
     * @param network    network key ({@code awin}...)
     * @param datasource datasource name (the {@code merchant} of a click)
     * @return {@code true} when the datasource is known to carry a programme of this network, so that a missing
     *         sale for it is a real zero rather than a missing figure
     */
    boolean hasProgramme(String network, String datasource);

    /**
     * @return a resolver that knows no datasource
     */
    static MerchantDatasourceResolver none() {
        return new MerchantDatasourceResolver() {
            @Override
            public Optional<String> datasourceOf(String network, String advertiserId) {
                return Optional.empty();
            }

            @Override
            public boolean hasProgramme(String network, String datasource) {
                return false;
            }
        };
    }
}
