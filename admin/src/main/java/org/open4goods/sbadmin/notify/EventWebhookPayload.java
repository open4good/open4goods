package org.open4goods.sbadmin.notify;

/**
 * Closed JSON body sent to the event-gateway. Deliberately carries only these eight fields: no
 * {@code details}, exception text, health URL, or other instance metadata, since the receiving
 * side has root-level Ops access.
 */
public record EventWebhookPayload(
        String source,
        String environment,
        String application,
        String instanceId,
        String from,
        String to,
        String at,
        String eventId) {
}
