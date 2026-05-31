package org.open4goods.services.feedservice.service;

import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.open4goods.commons.config.yml.datasource.DataSourceProperties;
import org.open4goods.commons.services.DataSourceConfigService;
import org.open4goods.model.affiliation.AffiliationPartner;
import org.open4goods.model.affiliation.AffiliationProgram;
import org.open4goods.model.affiliation.AffiliationPromotion;
import org.open4goods.model.affiliation.AffiliationTransaction;
import org.open4goods.model.helper.IdHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Updated FeedService orchestration class.
 * <p>
 * This class maintains the original external contract:
 * - {@code fetchFeeds()} to load and fetch all feeds,
 * - {@code fetchFeedsByUrl(String)} to fetch feeds by URL,
 * - {@code fetchFeedsByKey(String)} to fetch feeds by key,
 * - {@code getFeedsByDatasourceName(String)} to fetch feeds by datasource name, and
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
        	partners.add(toPartner(ds));
        }
    }




    /**
     * Aggregates datasource properties from all configured feed services and orphan datasource configurations.
     *
     * @return a set of datasource properties
     */
    public Set<DataSourceProperties> getFeedsUrl() {
        return getFeedsUrl(null);
    }

    /**
     * Aggregates datasource properties from matching feed services and orphan datasource configurations.
     *
     * @param providerName optional affiliation provider name filter
     * @return a set of datasource properties
     */
    public Set<DataSourceProperties> getFeedsUrl(String providerName) {
        Set<DataSourceProperties> result = new HashSet<>();

        // Delegate to each feed service implementation.
        for (AbstractFeedService service : feedServices) {
            try {
                if (!matchesProvider(service, providerName)) {
                    continue;
                }
                Set<DataSourceProperties> datasources = service.getDatasources();
                if (datasources != null) {
                    result.addAll(datasources);
                }
            } catch(Exception e) {
                logger.error("Error loading datasources from {}: ", service.getClass().getName(), e);
            }
        }

        // Add orphan feeds that do not have a defined feed key.
        if (isBlank(providerName)) {
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
        }

        return result;
    }

    /**
     * Returns datasources matching the provided datasource or provider name.
     *
     * @param datasourceName datasource/provider name to match
     * @return matching datasource properties
     */
    public Set<DataSourceProperties> getFeedsByDatasourceName(String datasourceName)
    {
        return getFeedsByDatasourceName(datasourceName, null);
    }

    /**
     * Returns datasources matching the provided datasource/provider name and optional affiliation provider.
     *
     * @param datasourceName datasource/provider name to match
     * @param providerName optional affiliation provider name filter
     * @return matching datasource properties
     */
    public Set<DataSourceProperties> getFeedsByDatasourceName(String datasourceName, String providerName)
    {
        String cleanedName = normalizeDatasourceName(datasourceName);
        Set<DataSourceProperties> result = new HashSet<>();
        for (DataSourceProperties ds : getFeedsUrl(providerName)) {
            try {
                String configName = normalizeDatasourceName(ds.getDatasourceConfigName());
                String dsName = normalizeDatasourceName(ds.getName());
                if (cleanedName.equals(configName) || cleanedName.equals(dsName)) {
                    result.add(ds);
                    logger.info("Matched feed: {}", ds);
                }
            } catch (Exception e) {
                logger.error("Error matching feed {}: ", ds, e);
            }
        }
        return result;
    }

    private String normalizeDatasourceName(String datasourceName)
    {
        if (datasourceName == null) {
            return "";
        }
        return IdHelper.azCharAndDigits(datasourceName).toLowerCase();
    }




	public Set<AffiliationPartner> getPartners() {
		return partners;
	}

    /**
     * Returns affiliation partners, optionally limited to one provider.
     *
     * @param providerName optional affiliation provider name filter
     * @return affiliation partners
     */
    public Set<AffiliationPartner> getPartners(String providerName) {
        if (isBlank(providerName)) {
            return partners;
        }
        return getFeedsUrl(providerName).stream().map(this::toPartner).collect(java.util.stream.Collectors.toSet());
    }




	public void setPartners(Set<AffiliationPartner> partners) {
		this.partners = partners;
	}

	public List<AbstractFeedService> getProviders() {
		return feedServices;
	}

	public Collection<AffiliationProgram> getPrograms() {
        return getPrograms(null);
    }

	public Collection<AffiliationProgram> getPrograms(String providerName) {
		Set<AffiliationProgram> all = new LinkedHashSet<>();
		for (AbstractFeedService service : feedServices) {
			try {
                if (!matchesProvider(service, providerName)) {
                    continue;
                }
				Collection<AffiliationProgram> programs = service.getPrograms();
				if (programs != null) {
					all.addAll(programs);
				}
			} catch (Exception e) {
				logger.error("Error loading programs from provider {}: ", service.getProviderName(), e);
			}
		}
		return all;
	}

	public Collection<AffiliationPromotion> getPromotions() {
        return getPromotions(null);
    }

	public Collection<AffiliationPromotion> getPromotions(String providerName) {
		Set<AffiliationPromotion> all = new LinkedHashSet<>();
		for (AbstractFeedService service : feedServices) {
			try {
                if (!matchesProvider(service, providerName)) {
                    continue;
                }
				Collection<AffiliationPromotion> promotions = service.getPromotions();
				if (promotions != null) {
					all.addAll(promotions);
				}
			} catch (Exception e) {
				logger.error("Error loading promotions from provider {}: ", service.getProviderName(), e);
			}
		}
		return all;
	}

	public Collection<AffiliationTransaction> getTransactions(Instant from, Instant to) {
        return getTransactions(from, to, null);
    }

	public Collection<AffiliationTransaction> getTransactions(Instant from, Instant to, String providerName) {
		Set<AffiliationTransaction> all = new LinkedHashSet<>();
		for (AbstractFeedService service : feedServices) {
			try {
                if (!matchesProvider(service, providerName)) {
                    continue;
                }
				Collection<AffiliationTransaction> transactions = service.getTransactions(from, to);
				if (transactions != null) {
					all.addAll(transactions);
				}
			} catch (Exception e) {
				logger.error("Error loading transactions from provider {}: ", service.getProviderName(), e);
			}
		}
		return all;
	}

	public String buildTrackingLink(String providerName, String programId, String targetUrl, Map<String, String> subIds) {
		if (providerName == null) {
			return targetUrl;
		}
		for (AbstractFeedService service : feedServices) {
			if (providerName.equalsIgnoreCase(service.getProviderName())) {
				try {
					return service.buildTrackingLink(programId, targetUrl, subIds);
				} catch (Exception e) {
					logger.error("Error building tracking link for provider {}: ", providerName, e);
				}
			}
		}
		return targetUrl;
	}

    private boolean matchesProvider(AbstractFeedService service, String providerName) {
        if (isBlank(providerName)) {
            return true;
        }
        return providerName.equalsIgnoreCase(service.getProviderName());
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private AffiliationPartner toPartner(DataSourceProperties ds) {
        AffiliationPartner p = new AffiliationPartner();
        p.setId(ds.getName());
        p.setName(ds.getName());
        p.setLogoUrl(ds.getLogo());
        p.setAffiliationLink(ds.getAffiliatedPortalUrl());
        p.setPortalUrl(ds.getPortalUrl());
        return p;
    }
}
