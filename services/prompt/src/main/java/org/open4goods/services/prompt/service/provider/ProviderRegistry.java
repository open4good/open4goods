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

    private final Map<GenAiServiceType, GenAiProvider> providers;

    public ProviderRegistry(List<GenAiProvider> providers) {
        this.providers = providers.stream()
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
