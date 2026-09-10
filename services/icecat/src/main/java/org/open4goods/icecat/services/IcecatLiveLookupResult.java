package org.open4goods.icecat.services;

import java.util.Optional;

import org.open4goods.icecat.model.IcecatLiveApiResponse.IceDataItem;

/**
 * Neutral outcome of an {@link IcecatLiveClient} lookup, decoupled from the HTTP status codes
 * and JSON-parsing exceptions that produced it so callers never need to know about
 * {@code RestClient} or Jackson.
 */
public final class IcecatLiveLookupResult {

    /** Discriminates the outcome without exposing the underlying HTTP/parsing mechanics. */
    public enum Status {
        /** A product was found and parsed. */
        FOUND,
        /** Icecat has no data for this GTIN. */
        NOT_FOUND,
        /** Icecat restricts this GTIN to an upgraded plan. */
        RESTRICTED,
        /** The call failed unexpectedly (network, unrecognized schema, etc.). */
        ERROR
    }

    private final Status status;
    private final IceDataItem product;
    private final String errorMessage;

    private IcecatLiveLookupResult(Status status, IceDataItem product, String errorMessage) {
        this.status = status;
        this.product = product;
        this.errorMessage = errorMessage;
    }

    public static IcecatLiveLookupResult found(IceDataItem product) {
        return new IcecatLiveLookupResult(Status.FOUND, product, null);
    }

    public static IcecatLiveLookupResult notFound() {
        return new IcecatLiveLookupResult(Status.NOT_FOUND, null, null);
    }

    public static IcecatLiveLookupResult restricted() {
        return new IcecatLiveLookupResult(Status.RESTRICTED, null, null);
    }

    public static IcecatLiveLookupResult error(String message) {
        return new IcecatLiveLookupResult(Status.ERROR, null, message);
    }

    public Status status() {
        return status;
    }

    public Optional<IceDataItem> product() {
        return Optional.ofNullable(product);
    }

    public Optional<String> errorMessage() {
        return Optional.ofNullable(errorMessage);
    }
}
