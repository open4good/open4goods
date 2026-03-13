package org.open4goods.services.productalert.controller;

import jakarta.validation.Valid;
import org.open4goods.services.productalert.dto.SubscriptionDto;
import org.open4goods.services.productalert.dto.SubscriptionUpsertRequest;
import org.open4goods.services.productalert.dto.UserDto;
import org.open4goods.services.productalert.dto.UserUpsertRequest;
import org.open4goods.services.productalert.service.ProductAlertService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public REST endpoints for product-alert registration and subscriptions.
 */
@RestController
@RequestMapping("/v1")
public class ProductAlertController
{
    private final ProductAlertService productAlertService;

    /**
     * Creates the controller.
     *
     * @param productAlertService product alert service
     */
    public ProductAlertController(ProductAlertService productAlertService)
    {
        this.productAlertService = productAlertService;
    }

    /**
     * Upserts a user by email.
     *
     * @param request user request
     * @return normalized user state
     */
    @PostMapping("/users")
    public ResponseEntity<UserDto> upsertUser(@Valid @RequestBody UserUpsertRequest request)
    {
        return ResponseEntity.ok(productAlertService.upsertUser(request));
    }

    /**
     * Upserts a product subscription.
     *
     * @param request subscription request
     * @return normalized subscription state
     */
    @PostMapping("/subscriptions")
    public ResponseEntity<SubscriptionDto> upsertSubscription(@Valid @RequestBody SubscriptionUpsertRequest request)
    {
        return ResponseEntity.ok(productAlertService.upsertSubscription(request));
    }
}
