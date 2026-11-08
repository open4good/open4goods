package org.open4goods.sbadmin.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

import de.codecentric.boot.admin.server.domain.entities.InstanceRepository;

import org.open4goods.sbadmin.config.properties.SbaEventWebhookProperties;
import org.open4goods.sbadmin.notify.EventWebhookNotifier;

/**
 * Registers {@link EventWebhookNotifier} as a {@code Notifier} bean, composed by SBA alongside the
 * standard {@code MailNotifier}.
 */
@Configuration
@EnableConfigurationProperties(SbaEventWebhookProperties.class)
public class EventWebhookNotifierConfig {

    @Bean
    EventWebhookNotifier eventWebhookNotifier(InstanceRepository repository, WebClient.Builder webClientBuilder,
            SbaEventWebhookProperties properties) {
        return new EventWebhookNotifier(repository, webClientBuilder, properties);
    }
}
