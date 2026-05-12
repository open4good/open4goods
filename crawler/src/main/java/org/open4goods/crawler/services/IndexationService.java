package org.open4goods.crawler.services;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.open4goods.commons.model.dto.api.IndexationResponse;
import org.open4goods.model.constants.UrlConstants;
import org.open4goods.model.datafragment.DataFragment;
import org.open4goods.model.exceptions.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

public class IndexationService {

    private static final Logger log = LoggerFactory.getLogger(IndexationService.class);

    protected Map<String, Long> byProviderCounters = new ConcurrentHashMap<>();

    private String indexationEndpoint;
    private String apiKey;
    private final RestTemplate restTemplate = new RestTemplate();

    public IndexationService() {
    }

    public IndexationService(final String indexationEndpoint, final String apiKey) {
        this.indexationEndpoint = indexationEndpoint;
        this.apiKey = apiKey;
    }

    public void index(final DataFragment data, final String datasourceConfigName) throws ValidationException {
        byProviderCounters.compute(datasourceConfigName, (k, v) -> v == null ? 1 : v + 1);
        indexInternal(data);
    }

    protected void indexInternal(final DataFragment data) throws ValidationException {
        log.error("REMOTE INDEXATION DISABLED FOR NOW — should not reach this code");
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(UrlConstants.APIKEY_PARAMETER, apiKey);
        try {
            restTemplate.postForObject(indexationEndpoint, new HttpEntity<>(data, headers), IndexationResponse.class);
        } catch (final RestClientException e) {
            log.error("Cannot send {} to master API indexation endpoint: {}", data, e.getMessage());
        }
    }

    public Long getIndexed(final String dataSourceName) {
        final Long ret = byProviderCounters.get(dataSourceName);
        return ret == null ? 0L : ret;
    }

    public void clearIndexedCounter(final String providerName) {
        byProviderCounters.remove(providerName);
    }
}
