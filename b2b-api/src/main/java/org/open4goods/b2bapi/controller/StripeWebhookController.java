package org.open4goods.b2bapi.controller;

import org.open4goods.b2bapi.service.StripeWebhookService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller to receive raw Stripe webhook callbacks.
 */
@RestController
@RequestMapping("/api/v1/billing/stripe")
public class StripeWebhookController {

    private final StripeWebhookService stripeWebhookService;

    public StripeWebhookController(final StripeWebhookService stripeWebhookService) {
        this.stripeWebhookService = stripeWebhookService;
    }

    /**
     * Webhook endpoint to receive asynchronous events from Stripe.
     *
     * @param payload raw webhook JSON body
     * @param sigHeader Stripe-Signature header
     */
    @PostMapping("/webhook")
    public void webhook(
            @RequestBody final String payload,
            @RequestHeader("Stripe-Signature") final String sigHeader) {
        stripeWebhookService.processWebhook(payload, sigHeader);
    }
}
