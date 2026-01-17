package org.open4goods.services.prompt.service.provider;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.open4goods.services.prompt.config.GenAiServiceType;
import org.springframework.stereotype.Component;

/**
 * Registry for GenAI providers.
 */
@Component
public class ProviderRegistry {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ProviderRegistry.class);

    private final Map<GenAiServiceType, GenAiProvider> providers;

    public ProviderRegistry(List<GenAiProvider> providers) {
        logger.info("DEBUG: ProviderRegistry initializing with {} providers: {}", providers.size(), providers);
        this.providers = providers.stream()
                .map(p -> {
                    logger.info("DEBUG: Registering provider: {} for service: {}", p.getClass().getSimpleName(), p.service());
                    return p;
                })
                .collect(Collectors.toMap(GenAiProvider::service, Function.identity()));
    }

    public GenAiProvider getProvider(GenAiServiceType serviceType) {
        GenAiProvider provider = providers.get(serviceType);
        if (provider == null) {
            throw new IllegalStateException("No provider configured for " + serviceType);
        }
        return provider;
    }

    public boolean hasProvider(GenAiServiceType serviceType) {
        return providers.containsKey(serviceType);
    }
}
