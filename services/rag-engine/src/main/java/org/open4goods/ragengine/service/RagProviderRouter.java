package org.open4goods.ragengine.service;

import org.open4goods.ragengine.config.RagEngineProperties;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Resolves configured providers by explicit use-case contracts.
 */
public class RagProviderRouter
{
    private final RagEngineProperties properties;
    private final Map<String, RagEngineProperties.Provider> providersByName;

    public RagProviderRouter(final RagEngineProperties properties)
    {
        this.properties = properties;
        this.providersByName = properties.getProviders().stream()
                .collect(Collectors.toMap(RagEngineProperties.Provider::getName, Function.identity()));
    }

    public RagEngineProperties.Provider chatProvider()
    {
        return resolve(properties.getUseCases().getChatProvider());
    }

    public RagEngineProperties.Provider embeddingProvider()
    {
        return resolve(properties.getUseCases().getEmbeddingProvider());
    }

    public RagEngineProperties.Provider summaryProvider()
    {
        return resolve(properties.getUseCases().getSummaryProvider());
    }

    private RagEngineProperties.Provider resolve(final String providerName)
    {
        final var provider = providersByName.get(providerName);
        if (provider == null)
        {
            throw new IllegalArgumentException("Unknown rag-engine provider: " + providerName);
        }
        return provider;
    }
}
