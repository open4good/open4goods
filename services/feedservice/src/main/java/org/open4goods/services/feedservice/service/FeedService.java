package org.open4goods.services.feedservice.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.open4goods.commons.config.yml.datasource.DataSourceProperties;
import org.open4goods.commons.services.DataSourceConfigService;
import org.open4goods.model.affiliation.AffiliationPartner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Updated FeedService orchestration class.
 * <p>
 * This class maintains the original external contract:
 * - {@code fetchFeeds()} to load and fetch all feeds,
 * - {@code fetchFeedsByUrl(String)} to fetch feeds by URL,
 * - {@code fetchFeedsByKey(String)} to fetch feeds by key, and
 * - {@code getFeedsUrl()} to aggregate datasource properties.
 * </p>
 * <p>
 * It delegates the loading of datasource properties to the dedicated implementations of {@link AbstractFeedService}
 * (for example, AwinFeedService and EffiliationFeedService) and merges these results with orphan feeds from the
 * DataSourceConfigService.
 * </p>
 */
public class FeedService {

    private static final Logger logger = LoggerFactory.getLogger(FeedService.class);

    // A list of all concrete feed service implementations (e.g., AwinFeedService, EffiliationFeedService)
    private final List<AbstractFeedService> feedServices;

    // Service to retrieve existing datasource configurations (used for orphan feeds)
    private final DataSourceConfigService dataSourceConfigService;

    private Set<AffiliationPartner> partners = new HashSet<>();

    /**
     * Constructor.
     *
     * @param feedServices list of concrete feed service implementations
     * @param dataSourceConfigService service providing datasource configurations
     * @param fetchingService service to start feed fetching operations
     */
    public FeedService(List<AbstractFeedService> feedServices,
                       DataSourceConfigService dataSourceConfigService) {
        this.feedServices = feedServices;
        this.dataSourceConfigService = dataSourceConfigService;
//        this.fetchingService = fetchingService;

        // TODO : Forcing load, in order feeds to be availlable to other service (datasourceconfig)
        // TODO : Not scheduled
        Set<DataSourceProperties> feeds = getFeedsUrl();
        for (DataSourceProperties ds : feeds) {
        	AffiliationPartner p = new AffiliationPartner();
        	p.setId(ds.getName());
        	p.setName(ds.getName());
        	p.setLogoUrl(ds.getLogo());
        	p.setAffiliationLink(ds.getAffiliatedPortalUrl());
        	partners.add(p);
        }
    }




    /**
     * Aggregates datasource properties from all configured feed services and orphan datasource configurations.
     *
     * @return a set of datasource properties
     */
    public Set<DataSourceProperties> getFeedsUrl() {
        Set<DataSourceProperties> result = new HashSet<>();

        // Delegate to each feed service implementation.
        for (AbstractFeedService service : feedServices) {
            try {
                result.addAll(service.getDatasources());
            } catch(Exception e) {
                logger.error("Error loading datasources from {}: ", service.getClass().getName(), e);
            }
        }

        // Add orphan feeds that do not have a defined feed key.
        dataSourceConfigService.datasourceConfigs().forEach((k, v) -> {
            try {
                if (v.getCsvDatasource() != null && (v.getFeedKey() == null || v.getFeedKey().trim().isEmpty())) {
                    logger.info("Adding orphan feed: {}", k);
                    v.setDatasourceConfigName(k);
                    result.add(v);
                }
            } catch(Exception e) {
                logger.error("Error processing orphan feed {}: ", k, e);
            }
        });

        return result;
    }




	public Set<AffiliationPartner> getPartners() {
		return partners;
	}




	public void setPartners(Set<AffiliationPartner> partners) {
		this.partners = partners;
	}



}
