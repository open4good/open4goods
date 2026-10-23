package org.open4goods.services.remotefilecaching.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.open4goods.model.helper.IdHelper;
import org.open4goods.services.remotefilecaching.config.CacheResourceConfig;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

import com.sun.net.httpserver.HttpServer;

/**
 * Tests cache file naming and diagnostic URL handling for remote resource caching.
 */
class RemoteFileCachingServiceTest {

    private static final String SECRET = "secret-key";

    @TempDir
    Path tempDir;

    /**
     * Ensures the cache file name is a SHA-256 that keeps no fragment of a URL carrying a key.
     */
    @Test
    void namesCacheFilesAfterTheSha256OfTheUrl() {
        String name = RemoteFileCachingService.cacheFileName("https://api.effinity.fr/apiv3/" + SECRET + "/publisher/programs?page=1");

        assertThat(name).matches("[0-9a-f]{64}");
        assertThat(name).doesNotContain(SECRET).doesNotContain("secretkey").doesNotContain("effinity");
        // SHA-256 of "abc", from FIPS 180-2
        assertThat(RemoteFileCachingService.cacheFileName("abc"))
                .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
    }

    /**
     * Ensures a downloaded resource is stored under its hashed name, and that nothing in the cache folder or in
     * the logs reveals the key carried by its URL.
     */
    @Test
    void cachesAResourceWithoutExposingTheKeyOfItsUrl() throws Exception {
        String url = remoteUrl("apiv3/" + SECRET + "/feed.json", "v1");
        ListAppender<ILoggingEvent> appender = captureLogs();
        File cached;
        try {
            cached = newService().getResource(url, 1);
        } finally {
            releaseLogs(appender);
        }

        assertThat(cached).hasName(RemoteFileCachingService.cacheFileName(url)).hasContent("v1");
        assertThat(cacheFolder().toFile().list()).containsExactly(cached.getName());
        assertThat(appender.list).isNotEmpty();
        assertThat(appender.list).allSatisfy(event -> assertThat(event.getFormattedMessage())
                .doesNotContain(SECRET)
                .doesNotContain("secretkey"));
    }

    /**
     * Ensures distinct URLs get distinct files, and that a cached copy is reused until its refresh period ends.
     */
    @Test
    void reusesTheCachedCopyUntilItsRefreshPeriodEnds() throws Exception {
        String url = remoteUrl("a/feed.csv", "v1");
        String otherUrl = remoteUrl("b/feed.csv", "other");
        RemoteFileCachingService service = newService();

        File cached = service.getResource(url, 1);
        File other = service.getResource(otherUrl, 1);
        assertThat(other).isNotEqualTo(cached).hasContent("other");

        // Within the refresh period: the remote change is not seen
        writeRemote("a/feed.csv", "v2");
        assertThat(service.getResource(url, 1)).isEqualTo(cached).hasContent("v1");

        // Past the refresh period: the resource is downloaded again
        assertThat(cached.setLastModified(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(2))).isTrue();
        assertThat(service.getResource(url, 1)).isEqualTo(cached).hasContent("v2");
    }

    /**
     * Ensures a refresh period counted in years does not overflow: the stale cache fallback of callers relies on it.
     */
    @Test
    void keepsACopyForAVeryLongRefreshPeriod() throws Exception {
        String url = remoteUrl("long/feed.csv", "v1");
        File cached = newService().getResource(url, Integer.MAX_VALUE);
        assertThat(cached.setLastModified(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(400))).isTrue();
        writeRemote("long/feed.csv", "v2");

        assertThat(newService().getResource(url, Integer.MAX_VALUE)).hasContent("v1");
    }

    /**
     * Ensures a copy left under the former URL-derived name is moved to the hashed name, keeping its age, instead of
     * being downloaded again or left behind.
     */
    @Test
    void adoptsACopyNamedAfterItsUrl() throws Exception {
        String url = remoteUrl("apiv3/" + SECRET + "/legacy.json", "remote");
        Files.createDirectories(cacheFolder());
        Path legacy = cacheFolder().resolve(IdHelper.getHashedName(url));
        Files.writeString(legacy, "legacy");
        Path legacyTmp = cacheFolder().resolve("tmp-" + IdHelper.getHashedName(url));
        Files.writeString(legacyTmp, "partial");

        File cached = newService().getResource(url, 1);

        assertThat(cached).hasName(RemoteFileCachingService.cacheFileName(url)).hasContent("legacy");
        assertThat(cacheFolder().toFile().list()).containsExactly(cached.getName());
    }

    /**
     * Ensures a former URL-derived copy past its refresh period is replaced by a fresh download, then removed.
     */
    @Test
    void refreshesAnOutdatedCopyNamedAfterItsUrl() throws Exception {
        String url = remoteUrl("old/feed.csv", "remote");
        Files.createDirectories(cacheFolder());
        Path legacy = cacheFolder().resolve(IdHelper.getHashedName(url));
        Files.writeString(legacy, "legacy");
        assertThat(legacy.toFile().setLastModified(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(3))).isTrue();

        File cached = newService().getResource(url, 1);

        assertThat(cached).hasContent("remote");
        assertThat(cacheFolder().toFile().list()).containsExactly(cached.getName());
    }

    /**
     * Ensures a former URL-derived copy is removed once {@code retrieve} downloads the resource.
     */
    @Test
    void retrieveRemovesTheCopyNamedAfterTheUrl() throws Exception {
        String url = remoteUrl("retrieve/catalog.csv", "remote");
        Files.createDirectories(cacheFolder());
        Files.writeString(cacheFolder().resolve(IdHelper.getHashedName(url)), "legacy");
        CacheResourceConfig conf = new CacheResourceConfig();
        conf.setUrl(url);

        File cached = newService().retrieve(conf);

        assertThat(cached).hasName(RemoteFileCachingService.cacheFileName(url)).hasContent("remote");
        assertThat(cacheFolder().toFile().list()).containsExactly(cached.getName());
    }

    /**
     * Ensures a failed download raises an exception that quotes neither the URL nor its key, cause included.
     */
    @Test
    void failedDownloadsDoNotQuoteTheUrl() {
        CacheResourceConfig conf = new CacheResourceConfig();
        conf.setUrl(tempDir.resolve("remote/apiv3/" + SECRET + "/missing.json").toUri().toString());

        Throwable thrown = catchThrowable(() -> newService().retrieve(conf));

        assertThat(thrown).isNotNull();
        StringWriter trace = new StringWriter();
        thrown.printStackTrace(new PrintWriter(trace));
        assertThat(trace.toString()).doesNotContain(SECRET).contains(FileNotFoundException.class.getName());
    }

    /**
     * Ensures a failed feed download raises an exception that quotes neither the URL nor its key: the JDK reports an
     * HTTP 404 with a {@link FileNotFoundException} whose message is the URL.
     */
    @Test
    void failedTmpDownloadsDoNotQuoteTheUrl() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            exchange.sendResponseHeaders(404, -1);
            exchange.close();
        });
        server.start();
        try {
            String url = "http://127.0.0.1:" + server.getAddress().getPort() + "/apiv3/" + SECRET + "/feed.csv";

            Throwable thrown = catchThrowable(() -> newService().downloadToTmpFile(url, "feed"));

            assertThat(thrown).isInstanceOf(IOException.class);
            StringWriter trace = new StringWriter();
            thrown.printStackTrace(new PrintWriter(trace));
            assertThat(trace.toString()).doesNotContain(SECRET).contains(FileNotFoundException.class.getName());
        } finally {
            server.stop(0);
        }
    }

    /**
     * Ensures the HTTP status code of a failure survives the redaction of its message.
     */
    @Test
    void redactedFailuresKeepTheHttpStatusCode() {
        IOException redacted = RemoteFileCachingService.redactedCause(new IOException(
                "Server returned HTTP response code: 403 for URL: https://api.example.test/apiv3/" + SECRET + "/x"));

        assertThat(redacted).hasMessage("java.io.IOException (HTTP 403)").hasNoCause();
    }

    /**
     * Ensures path segments and query values, which can contain credentials, never reach logs.
     */
    @Test
    void redactsPathAndQueryFromDiagnosticUrls() {
        assertThat(RemoteFileCachingService.loggableUrl(
                "https://example.test/apiv3/" + SECRET + "/resource.json?accessToken=secret&signature=also-secret"))
                .isEqualTo("https://example.test/<redacted>");
        assertThat(RemoteFileCachingService.loggableUrl("https://user:password@example.test:8443/resource"))
                .isEqualTo("https://example.test:8443/<redacted>");
        assertThat(RemoteFileCachingService.loggableUrl("https://example.test/")).isEqualTo("https://example.test");
        assertThat(RemoteFileCachingService.loggableUrl("not a url")).isEqualTo("<redacted>");
        assertThat(RemoteFileCachingService.loggableUrl(null)).isNull();
    }

    /**
     * Ensures a URL carrying a secret as a path segment is replaced by its log label, including when the download
     * fails, and that the line names the cache file.
     */
    @Test
    void logsTheLabelInsteadOfAUrlCarryingASecretInItsPath() throws Exception {
        // Port 1 is never listening: the download fails and the failure is logged.
        String url = "http://127.0.0.1:1/apiv3/path-secret-value/resource?page=1";
        ListAppender<ILoggingEvent> appender = captureLogs();
        try {
            newService().getResource(url, 1, "http://127.0.0.1:1/apiv3/{apikey}/resource");
        } finally {
            releaseLogs(appender);
        }

        assertThat(appender.list).isNotEmpty();
        assertThat(appender.list).allSatisfy(event -> assertThat(event.getFormattedMessage())
                .doesNotContain("path-secret-value")
                .doesNotContain("pathsecretvalue"));
        assertThat(appender.list).anySatisfy(event -> assertThat(event.getFormattedMessage())
                .contains("{apikey}")
                .contains(RemoteFileCachingService.cacheFileName(url)));
    }

    private RemoteFileCachingService newService() {
        return new RemoteFileCachingService(cacheFolder().toString());
    }

    private Path cacheFolder() {
        return tempDir.resolve("cache");
    }

    /**
     * Writes a "remote" resource under the temporary folder and returns its {@code file:} URL, which the service
     * downloads like an HTTP one.
     */
    private String remoteUrl(String relativePath, String content) throws IOException {
        return writeRemote(relativePath, content).toUri().toString();
    }

    private Path writeRemote(String relativePath, String content) throws IOException {
        Path remote = tempDir.resolve("remote").resolve(relativePath);
        Files.createDirectories(remote.getParent());
        return Files.writeString(remote, content, StandardCharsets.UTF_8);
    }

    private static ListAppender<ILoggingEvent> captureLogs() {
        Logger logger = (Logger) LoggerFactory.getLogger(RemoteFileCachingService.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        return appender;
    }

    private static void releaseLogs(ListAppender<ILoggingEvent> appender) {
        ((Logger) LoggerFactory.getLogger(RemoteFileCachingService.class)).detachAppender(appender);
    }
}
