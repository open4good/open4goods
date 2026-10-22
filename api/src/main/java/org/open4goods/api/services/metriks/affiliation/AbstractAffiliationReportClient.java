package org.open4goods.api.services.metriks.affiliation;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.springframework.util.StringUtils;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Shared HTTP and parsing helpers of the Metriks affiliation clients.
 * <p>
 * Every failure becomes an {@link AffiliationReportException} whose message names the network, the step and,
 * for an HTTP error, the status code only: never the request URL, a header, the response body or the message
 * of a lower-level exception, any of which could carry a credential.
 */
abstract class AbstractAffiliationReportClient implements AffiliationReportClient {

    /** Timeout of a single HTTP request. */
    static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);
    /** Timeout of the TCP connection. */
    static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    /** Longest network status echoed in an error message. */
    private static final int MAX_ECHOED_STATUS_LENGTH = 40;

    protected final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient;

    protected AbstractAffiliationReportClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    /**
     * @return an HTTP client with the Metriks connection timeout, following no redirect
     */
    static HttpClient defaultHttpClient() {
        return HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build();
    }

    /**
     * Sends a request and returns the body of a 2xx response.
     *
     * @param request request to send
     * @param step    short, secret-free name of the call, used in error messages
     * @return response body
     * @throws AffiliationReportException on timeout, I/O error, interruption or non-2xx status
     */
    protected String send(HttpRequest request, String step) throws AffiliationReportException {
        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (HttpTimeoutException e) {
            throw failure("délai dépassé (" + step + ")");
        } catch (IOException e) {
            throw failure("erreur réseau (" + step + ", " + e.getClass().getSimpleName() + ")");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw failure("appel interrompu (" + step + ")");
        }
        int status = response.statusCode();
        if (status < 200 || status >= 300) {
            throw failure("HTTP " + status + " (" + step + ")");
        }
        return response.body();
    }

    /**
     * @param body JSON text
     * @param step step name used in error messages
     * @return parsed tree
     * @throws AffiliationReportException when the body is not valid JSON
     */
    protected JsonNode readJson(String body, String step) throws AffiliationReportException {
        try {
            JsonNode root = objectMapper.readTree(body == null ? "" : body);
            if (root == null || root.isMissingNode()) {
                throw failure("réponse vide (" + step + ")");
            }
            return root;
        } catch (JacksonException e) {
            throw failure("réponse JSON illisible (" + step + ")");
        }
    }

    /**
     * Reads a decimal amount from a JSON number or a numeric string (a decimal comma is accepted).
     *
     * @return the amount, or {@code null} when the node is missing, null or blank
     * @throws AffiliationReportException when the value is not a number
     */
    protected BigDecimal decimalOrNull(JsonNode node, String field) throws AffiliationReportException {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        if (node.isNumber()) {
            return node.decimalValue();
        }
        String text = node.asString();
        if (!StringUtils.hasText(text)) {
            return null;
        }
        try {
            return new BigDecimal(text.trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            throw failure("montant illisible dans le champ " + field);
        }
    }

    /**
     * @return text of the node, or {@code null} when missing, null or blank
     */
    protected static String textOrNull(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        String text = node.asString();
        return StringUtils.hasText(text) ? text.trim() : null;
    }

    /**
     * Returns a short response value (status, date, currency) to quote in an error message. A long value is not
     * quoted, not even truncated: a truncated credential would escape the scrubbing of
     * {@link AffiliationMetriksService}.
     *
     * @return the value, or a placeholder when absent or too long
     */
    static String echo(String value) {
        if (value == null) {
            return "(absent)";
        }
        return value.length() > MAX_ECHOED_STATUS_LENGTH ? "(valeur de " + value.length() + " caractères)" : value;
    }

    /**
     * @return an exception whose message is prefixed with the network name
     */
    protected AffiliationReportException failure(String detail) {
        return new AffiliationReportException(displayName() + " : " + detail);
    }

    /**
     * @param properties pairs of (full property name, value)
     * @return names of the properties whose value is blank
     */
    protected static List<String> missing(String... properties) {
        List<String> names = new ArrayList<>();
        for (int i = 0; i + 1 < properties.length; i += 2) {
            if (!StringUtils.hasText(properties[i + 1])) {
                names.add(properties[i]);
            }
        }
        return names;
    }

    /**
     * @return the non-blank values among the given ones
     */
    protected static List<String> nonBlank(String... values) {
        return Arrays.stream(values).filter(Objects::nonNull).filter(StringUtils::hasText).toList();
    }
}
