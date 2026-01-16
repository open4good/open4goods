package org.open4goods.services.prompt.service.provider;

import org.open4goods.services.prompt.config.GenAiServiceType;
import reactor.core.publisher.Flux;

/**
 * Provider interface for GenAI implementations.
 */
public interface GenAiProvider {

    GenAiServiceType service();

    ProviderResult generateText(ProviderRequest request);

    Flux<ProviderEvent> generateTextStream(ProviderRequest request);
}
