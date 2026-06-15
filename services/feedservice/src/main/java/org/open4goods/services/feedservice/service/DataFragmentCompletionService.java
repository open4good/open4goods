package org.open4goods.services.feedservice.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.open4goods.commons.config.yml.datasource.DataSourceProperties;
import org.open4goods.model.attribute.Attribute;
import org.open4goods.model.attribute.ReferentielKey;
import org.open4goods.model.datafragment.DataFragment;
import org.open4goods.model.datafragment.ProviderSupportType;
import org.open4goods.model.helper.IdHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Completes feed-created data fragments before they enter the product store.
 */
public class DataFragmentCompletionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataFragmentCompletionService.class);

    public void complete(final DataFragment dataFragment, final String datasourceConfigName,
            final DataSourceProperties datasourceProperties, final Logger dedicatedLogger) {
        if (dataFragment == null) {
            LOGGER.error("Cannot complete a null dataFragment");
            return;
        }

        dataFragment.setDatasourceName(datasourceProperties.getName());
        long currentTimeMillis = System.currentTimeMillis();
        dataFragment.setLastIndexationDate(currentTimeMillis);
        if (dataFragment.getPrice() != null && dataFragment.getPrice().getTimeStamp() == null) {
            dataFragment.getPrice().setTimeStamp(currentTimeMillis);
        }
        dataFragment.setCreationDate(currentTimeMillis);
        dataFragment.setProviderSupportType(ProviderSupportType.CSV);
        dataFragment.setDatasourceConfigName(datasourceConfigName);
        dataFragment.setReferentielData(datasourceProperties.getReferentiel());

        extractReferentielAttributes(dataFragment, datasourceProperties, dedicatedLogger);
        extractBrandUid(dataFragment, datasourceProperties, dedicatedLogger);
        dataFragment.getResources().forEach(resource -> resource.setDatasourceName(dataFragment.getDatasourceName()));

        if (!StringUtils.isEmpty(datasourceProperties.getAffiliationLinkPrefix())) {
            dataFragment.setAffiliatedUrl(datasourceProperties.getAffiliationLinkPrefix()
                    + URLEncoder.encode(dataFragment.getUrl(), StandardCharsets.UTF_8));
            if (!StringUtils.isEmpty(datasourceProperties.getAffiliationLinkSuffix())) {
                dataFragment.setAffiliatedUrl(dataFragment.getAffiliatedUrl()
                        + datasourceProperties.getAffiliationLinkSuffix());
            }
        }

        if (datasourceProperties.getDefaultItemCondition() != null && dataFragment.getProductState() == null) {
            dataFragment.setProductState(datasourceProperties.getDefaultItemCondition());
        }
    }

    public void extractReferentielAttributes(final DataFragment dataFragment,
            final DataSourceProperties datasourceConfig, final Logger dedicatedLogger) {
        final Iterator<Attribute> attrIterator = dataFragment.getAttributes().iterator();
        final Set<Attribute> toRemove = new HashSet<>();

        while (attrIterator.hasNext()) {
            final Attribute attribute = attrIterator.next();
            try {
                final String name = attribute.getName().toUpperCase();
                final ReferentielKey key = DataSourceProperties.getDefaultReferentielAttributes().get(name);
                if (key != null) {
                    if (dataFragment.getReferentielAttributes().get(key) == null) {
                        dataFragment.addReferentielAttribute(key, attribute.getRawValue());
                        dedicatedLogger.info("Adding referentiel attribute {}:{} from attribute {}",
                                key, attribute.getRawValue(), attribute.getName());
                    } else {
                        dedicatedLogger.info("Referentiel attribute {}:{} not added because attr with value {} already exists",
                                key, attribute.getRawValue(), attribute.getName(),
                                dataFragment.getReferentielAttributes().get(key));
                    }
                } else if (name.contains("EAN") || name.contains("GTIN") || name.contains("GENCOD")) {
                    dedicatedLogger.warn("Could consider adding referentiel attribute {}:{} from attribute name {}",
                            key, attribute.getRawValue(), attribute.getName());
                }
            } catch (final NoSuchElementException e) {
                dedicatedLogger.warn("Missing value when extracting referentiel attributes: {} > {}",
                        attribute.getName(), e.getMessage());
            }
        }

        dataFragment.getAttributes().removeAll(toRemove);
    }

    public void extractBrandUid(final DataFragment dataFragment, final DataSourceProperties datasourceConfig,
            final Logger dedicatedLogger) {
        if (!datasourceConfig.getExtractBrandUidFromName()) {
            return;
        }
        final Attribute model = dataFragment.getAttribute(ReferentielKey.MODEL.toString());
        if (model != null) {
            dedicatedLogger.warn("Already have MODEL {}, will skip extraction of this attribute from the name",
                    model.getRawValue());
            return;
        }

        String modelId = IdHelper.extractModelFromNames(dataFragment);
        if (modelId != null) {
            dataFragment.addReferentielAttribute(ReferentielKey.MODEL, modelId);
        }
    }
}
