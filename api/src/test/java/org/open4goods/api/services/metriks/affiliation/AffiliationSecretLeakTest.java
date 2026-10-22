package org.open4goods.api.services.metriks.affiliation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.open4goods.api.services.metriks.affiliation.AffiliationTestSupport.CLOCK;
import static org.open4goods.api.services.metriks.affiliation.AffiliationTestSupport.response;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.open4goods.api.dto.metriks.MetriksEvent;
import org.open4goods.api.services.metriks.affiliation.AffiliationTestSupport.FakeHttp;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import ch.qos.logback.core.read.ListAppender;
import tools.jackson.databind.ObjectMapper;

/**
 * Shows that no credential reaches a Metriks {@code errorMessage}, the serialised response or a log line, even when
 * the network echoes it in an error body, when a lower-level exception carries the request URL (the Effiliation key
 * travels in the query string) or when a status value happens to contain it.
 */
class AffiliationSecretLeakTest {

    private static final String AWIN_TOKEN = "AWINSECRET0123456789";
    private static final String EFFILIATION_KEY = "EFFISECRET0123456789";
    private static final List<String> SECRETS = List.of(AWIN_TOKEN, EFFILIATION_KEY);
    private static final String EFFILIATION_URL =
            "https://api.effinity.fr/apiv3/" + EFFILIATION_KEY + "/publisher/conversions?start=2026-10-01";

    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();
    private Logger root;
    private Level previousLevel;

    @BeforeEach
    void captureLogs() {
        root = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        previousLevel = root.getLevel();
        root.setLevel(Level.TRACE);
        ((Logger) LoggerFactory.getLogger("org.open4goods")).setLevel(Level.TRACE);
        appender.start();
        root.addAppender(appender);
    }

    @AfterEach
    void releaseLogs() {
        root.detachAppender(appender);
        root.setLevel(previousLevel);
        ((Logger) LoggerFactory.getLogger("org.open4goods")).setLevel(Level.WARN);
    }

    private List<MetriksEvent> collect(AffiliationReportClient... clients) {
        return new AffiliationMetriksService(List.of(clients), CLOCK).collectEvents();
    }

    private void assertNoSecret(List<MetriksEvent> events) {
        List<String> texts = new ArrayList<>();
        events.forEach(event -> texts.add(String.valueOf(event.getErrorMessage())));
        texts.add(new ObjectMapper().writeValueAsString(events));
        for (ILoggingEvent log : appender.list) {
            texts.add(log.getFormattedMessage());
            if (log.getThrowableProxy() != null) {
                texts.add(ThrowableProxyUtil.asString(log.getThrowableProxy()));
            }
        }
        for (String text : texts) {
            for (String secret : SECRETS) {
                assertThat(text).doesNotContain(secret);
            }
        }
        // The failures were reported, and logged.
        assertThat(events).anyMatch(event -> "error".equals(event.getStatus()));
        assertThat(appender.list).isNotEmpty();
    }

    @Test
    void httpErrorBodiesEchoingTheSecretsAreNeverReported() {
        List<MetriksEvent> events = collect(
                new AwinReportClient("100000", AWIN_TOKEN, new FakeHttp(request -> response(401,
                        "{\"error\":\"invalid token " + AWIN_TOKEN + "\"}")).client),
                new EffiliationReportClient(EFFILIATION_KEY, new FakeHttp(request -> response(500,
                        "Fatal error while processing " + EFFILIATION_URL)).client));

        assertNoSecret(events);
        assertThat(events.get(0).getErrorMessage()).isEqualTo("Awin : HTTP 401 (transactions)");
    }

    @Test
    void lowerLevelExceptionsCarryingTheUrlAreNeverReported() {
        List<MetriksEvent> events = collect(
                new EffiliationReportClient(EFFILIATION_KEY, new FakeHttp(request -> {
                    throw new IOException("HTTP/1.1 header parser received no bytes from " + EFFILIATION_URL);
                }).client),
                new AwinReportClient("100000", AWIN_TOKEN, new FakeHttp(request -> {
                    throw new IllegalArgumentException("Bearer " + AWIN_TOKEN);
                }).client));

        assertNoSecret(events);
    }

    @Test
    void unreadableResponsesContainingTheSecretAreNeverReported() {
        List<MetriksEvent> events = collect(
                new EffiliationReportClient(EFFILIATION_KEY, new FakeHttp(request -> response(200,
                        "<html>" + EFFILIATION_URL + "</html>")).client),
                new AwinReportClient("100000", AWIN_TOKEN, new FakeHttp(request -> response(200,
                        "<html>" + AWIN_TOKEN + "</html>")).client));

        assertNoSecret(events);
    }

    @Test
    void aStatusValueEchoingTheSecretIsScrubbed() {
        List<MetriksEvent> events = collect(new EffiliationReportClient(EFFILIATION_KEY, new FakeHttp(request -> response(200,
                "{\"conversions\":[{\"status\":\"" + EFFILIATION_KEY + "\",\"conversionDate\":\"2026-10-16T09:00:00\","
                        + "\"commissionAmount\":1}]}")).client));

        assertNoSecret(events);
        assertThat(events.get(0).getErrorMessage()).isEqualTo("Effiliation : statut inconnu : ***");
    }
}
