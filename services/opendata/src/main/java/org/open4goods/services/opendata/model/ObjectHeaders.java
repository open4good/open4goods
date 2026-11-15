package org.open4goods.services.opendata.model;

/**
 * HTTP headers and user metadata written with an object of the open data bucket. No other metadata is written: in
 * particular none of the local file (owner, mode, dates).
 *
 * @param contentType        {@code Content-Type}
 * @param contentDisposition {@code Content-Disposition}, null for none
 * @param cacheControl       {@code Cache-Control}, null for none
 * @param sha256             {@code sha256} user metadata ({@code x-amz-meta-sha256}), null for none
 */
public record ObjectHeaders(String contentType, String contentDisposition, String cacheControl, String sha256) {
}
