package org.open4goods.nudgerfrontapi.service.geoip;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.GZIPInputStream;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.open4goods.nudgerfrontapi.config.properties.GeoIpProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.maxmind.db.CHMCache;
import com.maxmind.geoip2.DatabaseReader;

/**
 * Downloads, caches and hot-swaps the MaxMind GeoLite2 City database for local
 * in-process IP lookups. Ported from infera's {@code GeoIpDatabaseManager},
 * slimmed to the City database only (no ASN).
 *
 * <p>The reader is memory-mapped from file and uses a bounded {@link CHMCache},
 * so the heap footprint stays small on the OOM-sensitive front-api. When the
 * database is unavailable the manager stays in degraded mode and callers fall
 * back to the geocode microservice.
 */
@Service
public class GeoIpDatabaseManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(GeoIpDatabaseManager.class);
    private static final String TEMP_PREFIX = "geoip-city";

    private final GeoIpProperties properties;
    private final DatabaseReaderFactory readerFactory;
    private final HttpClient httpClient = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
    private final AtomicReference<DatabaseReader> readerRef = new AtomicReference<>();
    private volatile Instant lastRefreshAt;
    private volatile String lastError;

    @Autowired
    public GeoIpDatabaseManager(GeoIpProperties properties) {
        this(properties, dbPath -> new DatabaseReader.Builder(dbPath.toFile()).withCache(new CHMCache()).build());
    }

    GeoIpDatabaseManager(GeoIpProperties properties, DatabaseReaderFactory readerFactory) {
        this.properties = properties;
        this.readerFactory = readerFactory;
        if (properties.enabled()) {
            loadExistingDatabase();
        }
    }

    @Scheduled(fixedDelayString = "${frontapi.geoip.refresh-ms:604800000}",
            initialDelayString = "${frontapi.geoip.initial-delay-ms:10000}")
    public synchronized void refreshIfStale() {
        if (!properties.enabled()) {
            return;
        }
        try {
            Path dbPath = Path.of(properties.dbPath());
            if (Files.exists(dbPath)) {
                Instant lastModified = Files.getLastModifiedTime(dbPath).toInstant();
                if (Instant.now().minusMillis(properties.refreshMs()).isBefore(lastModified)) {
                    ensureLoaded(dbPath);
                    return;
                }
            }
            downloadAndInstallDatabase(dbPath);
            ensureLoaded(dbPath);
            lastError = null;
        } catch (Exception ex) {
            lastError = ex.getMessage();
            LOGGER.warn("GeoIP DB refresh failed; staying in degraded mode (fallback to geocode service): {}",
                    ex.getMessage());
            LOGGER.debug("GeoIP refresh stacktrace", ex);
        }
    }

    public Optional<DatabaseReader> reader() {
        return Optional.ofNullable(readerRef.get());
    }

    public boolean available() {
        return readerRef.get() != null;
    }

    public Instant lastRefreshAt() {
        return lastRefreshAt;
    }

    public String lastError() {
        return lastError;
    }

    private void loadExistingDatabase() {
        Path dbPath = Path.of(properties.dbPath());
        if (Files.exists(dbPath)) {
            try {
                ensureLoaded(dbPath);
            } catch (IOException ex) {
                lastError = ex.getMessage();
                LOGGER.warn("Unable to load existing GeoIP DB from {}: {}", dbPath, ex.getMessage());
            }
        }
    }

    private void downloadAndInstallDatabase(Path dbPath) throws Exception {
        if (properties.licenseKey() == null || properties.licenseKey().isBlank()) {
            throw new IllegalStateException("frontapi.geoip.license-key is required for MaxMind download");
        }
        Path parent = dbPath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Path tempTarGz = Files.createTempFile(TEMP_PREFIX, ".tar.gz");
        Path tempDb = Files.createTempFile(TEMP_PREFIX, ".mmdb");

        String url = properties.downloadUrl()
                + "?edition_id=" + URLEncoder.encode(properties.editionId(), StandardCharsets.UTF_8)
                + "&license_key=" + URLEncoder.encode(properties.licenseKey(), StandardCharsets.UTF_8)
                + "&suffix=tar.gz";

        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).GET().build();
        HttpResponse<Path> response = httpClient.send(request, HttpResponse.BodyHandlers.ofFile(tempTarGz));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("MaxMind download failed with HTTP status " + response.statusCode());
        }

        extractMmdbFromTarGz(tempTarGz, tempDb);
        Files.move(tempDb, dbPath, StandardCopyOption.REPLACE_EXISTING);
        Files.deleteIfExists(tempTarGz);
    }

    private void extractMmdbFromTarGz(Path archivePath, Path outputDbPath) throws IOException {
        try (InputStream fileIn = Files.newInputStream(archivePath);
                GZIPInputStream gzipIn = new GZIPInputStream(fileIn);
                TarArchiveInputStream tarIn = new TarArchiveInputStream(gzipIn)) {
            TarArchiveEntry entry;
            while ((entry = tarIn.getNextEntry()) != null) {
                if (entry.isFile() && entry.getName().endsWith(".mmdb")) {
                    Files.copy(tarIn, outputDbPath, StandardCopyOption.REPLACE_EXISTING);
                    return;
                }
            }
        }
        throw new IOException("No .mmdb file found in downloaded archive");
    }

    private void ensureLoaded(Path dbPath) throws IOException {
        DatabaseReader current = readerRef.get();
        if (current != null) {
            current.close();
        }
        DatabaseReader loaded = readerFactory.create(dbPath);
        readerRef.set(loaded);
        lastRefreshAt = Instant.now();
        LOGGER.info("GeoIP City DB loaded from {} (buildDate={})", dbPath, loaded.getMetadata().buildTime());
    }

    /** Reader factory indirection, used to inject a fake reader in tests. */
    public interface DatabaseReaderFactory {
        DatabaseReader create(Path dbPath) throws IOException;
    }
}
