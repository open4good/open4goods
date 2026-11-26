package org.open4goods.services.opendata.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.open4goods.services.opendata.config.OpenDataConfig;
import org.open4goods.services.opendata.model.ColumnManifest;
import org.open4goods.services.opendata.model.DatasetManifest;
import org.open4goods.services.opendata.model.ObjectHeaders;
import org.open4goods.services.opendata.model.OpenDataGenerationStatus.State;
import org.open4goods.services.opendata.model.OpenDataManifest;
import org.open4goods.services.opendata.service.InMemoryOpenDataStorage.Stored;
import org.slf4j.LoggerFactory;
import org.springframework.util.unit.DataSize;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

/**
 * Publication on R2 (GOU-2289) against an in-memory bucket: order of the writes, read back of the sha256, budget guard
 * and failures that leave the published version intact.
 */
class OpenDataPublisherTest {

    private static final String OLD = "old version";

    @TempDir
    Path tempDir;

    private OpenDataConfig config;
    private SimpleMeterRegistry meterRegistry;
    private InMemoryOpenDataStorage storage;
    private AtomicInteger storagesCreated;
    private OpenDataPublisher publisher;
    private List<OpenDataPublisher.Dataset> datasets;
    private Path globalManifest;

    @BeforeEach
    void setUp() throws IOException {
        config = new OpenDataConfig();
        config.setRootFolder(tempDir.toString());
        config.getPublication().setEnabled(true);
        Files.createDirectories(config.openDataDirectory());
        meterRegistry = new SimpleMeterRegistry();
        storage = new InMemoryOpenDataStorage();
        storagesCreated = new AtomicInteger();
        publisher = new OpenDataPublisher(config, meterRegistry, publication -> {
            storagesCreated.incrementAndGet();
            return storage;
        });

        // The version currently online, and an object of an interrupted publication.
        for (String key : List.of("gtin-open-data.zip", "isbn-open-data.zip", "gtin-manifest.json", "isbn-manifest.json",
                "manifest.json")) {
            storage.objects.put(key, new Stored(OLD.getBytes(StandardCharsets.UTF_8), null));
        }
        storage.objects.put(".staging/leftover.zip", new Stored(new byte[10], null));

        datasets = List.of(dataset("gtin", "gtin.zip", "gtin-open-data.zip", 3000),
                dataset("isbn", "isbn.zip", "isbn-open-data.zip", 2000));
        globalManifest = config.manifestFile().toPath();
        new OpenDataManifestStore().write(new OpenDataManifest(OpenDataManifest.SCHEMA_VERSION, "2026-11-22T03:00:00Z",
                OpenDataManifest.LICENSE, datasets.stream().map(OpenDataPublisher.Dataset::manifest).toList()),
                globalManifest.toFile());
        storage.operations.clear();
    }

    @Test
    void publishesZipsThenReadBackThenManifestsWithManifestJsonLastThenDeletesTemporaryObjects() throws Exception {
        assertThat(publisher.publish(datasets, globalManifest)).isTrue();

        assertThat(storage.operations).containsExactly(
                "list ",
                "upload .staging/gtin-open-data.zip",
                "upload .staging/isbn-open-data.zip",
                "read .staging/gtin-open-data.zip",
                "read .staging/isbn-open-data.zip",
                "copy .staging/gtin-open-data.zip gtin-open-data.zip",
                "head gtin-open-data.zip",
                "copy .staging/isbn-open-data.zip isbn-open-data.zip",
                "head isbn-open-data.zip",
                "put gtin-manifest.json",
                "put isbn-manifest.json",
                "put manifest.json",
                "delete .staging/leftover.zip",
                "delete .staging/gtin-open-data.zip",
                "delete .staging/isbn-open-data.zip",
                "list ");

        assertThat(storage.objects.keySet()).containsExactlyInAnyOrder("gtin-open-data.zip", "isbn-open-data.zip",
                "gtin-manifest.json", "isbn-manifest.json", "manifest.json");
        for (OpenDataPublisher.Dataset dataset : datasets) {
            Stored zip = storage.objects.get(dataset.manifest().file());
            assertThat(zip.content()).isEqualTo(Files.readAllBytes(dataset.zipFile()));
            assertThat(zip.headers()).isEqualTo(new ObjectHeaders("application/zip",
                    "attachment; filename=\"" + dataset.manifest().file() + "\"", "public, max-age=300",
                    dataset.manifest().sha256()));
            assertThat(storage.objects.get(dataset.manifestFile().getFileName().toString()).content())
                    .isEqualTo(Files.readAllBytes(dataset.manifestFile()));
        }
        Stored manifest = storage.objects.get("manifest.json");
        assertThat(manifest.content()).isEqualTo(Files.readAllBytes(globalManifest));
        assertThat(manifest.headers()).isEqualTo(new ObjectHeaders("application/json", null, "public, max-age=300", null));

        assertThat(publisher.status().state()).isEqualTo(State.SUCCEEDED);
        assertThat(publisher.status().bucketBytes()).isEqualTo(storage.totalBytes());
        assertThat(meterRegistry.get("opendata.publication.runs").tag("outcome", "success").counter().count()).isEqualTo(1);
        assertThat(meterRegistry.get("opendata.publication.failed").gauge().value()).isZero();
        assertThat(meterRegistry.get("opendata.publication.bucket.size").gauge().value()).isEqualTo(storage.totalBytes());
    }

    @Test
    void sha256MismatchOfTheTemporaryObjectLeavesThePublicVersionIntact() {
        storage.corruptUploads = true;

        assertThat(publisher.publish(datasets, globalManifest)).isFalse();

        assertOldVersionIntact();
        assertThat(storage.operations).noneMatch(operation -> operation.startsWith("copy") || operation.startsWith("put"));
        assertThat(storage.objects.keySet()).noneMatch(key -> key.startsWith(".staging/gtin")
                || key.startsWith(".staging/isbn"));
        assertFailed("read back has sha256");
    }

    @Test
    void budgetGuardRefusesBeforeAnyUpload() {
        // Kept objects (other than the replaced ones and the temporary ones) count, the new files are added.
        storage.objects.put("other.bin", new Stored(new byte[4000], null));
        config.getPublication().setMaxBucketSize(DataSize.ofBytes(10_000));

        assertThat(publisher.publish(datasets, globalManifest)).isFalse();

        assertThat(storage.operations).containsExactly("list ");
        assertOldVersionIntact();
        assertFailed("Budget guard");
    }

    @Test
    void budgetGuardIgnoresTheReplacedAndTemporaryObjects() {
        storage.objects.put("gtin-open-data.zip", new Stored(new byte[8000], null));
        storage.objects.put(".staging/leftover.zip", new Stored(new byte[8000], null));
        config.getPublication().setMaxBucketSize(DataSize.ofBytes(10_000));

        assertThat(publisher.publish(datasets, globalManifest)).isTrue();
    }

    @Test
    void budgetGuardWarnsAbove80PercentAndExposesTheProjectedRatio() {
        // GOU-2616: about 6,900 bytes after the publication, between 80 % and 100 % of 8,000.
        config.getPublication().setMaxBucketSize(DataSize.ofBytes(8_000));
        Logger logger = (Logger) LoggerFactory.getLogger(OpenDataPublisher.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            assertThat(publisher.publish(datasets, globalManifest)).isTrue();
        } finally {
            logger.detachAppender(appender);
        }

        assertThat(meterRegistry.get("opendata.publication.bucket.projected.ratio").gauge().value())
                .isGreaterThan(OpenDataPublisher.BUDGET_WARNING_RATIO).isLessThan(1);
        assertThat(appender.list).anySatisfy(event -> {
            assertThat(event.getLevel()).isEqualTo(Level.WARN);
            assertThat(event.getFormattedMessage()).contains("max-bucket-size");
        });
    }

    @Test
    void budgetGuardDoesNotWarnBelow80PercentAndExposesARefusedRatio() {
        Logger logger = (Logger) LoggerFactory.getLogger(OpenDataPublisher.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            assertThat(publisher.publish(datasets, globalManifest)).isTrue();
        } finally {
            logger.detachAppender(appender);
        }
        assertThat(appender.list).noneSatisfy(event -> assertThat(event.getLevel()).isEqualTo(Level.WARN));
        assertThat(meterRegistry.get("opendata.publication.bucket.projected.ratio").gauge().value()).isLessThan(0.8);

        config.getPublication().setMaxBucketSize(DataSize.ofBytes(1_000));
        assertThat(publisher.publish(datasets, globalManifest)).isFalse();
        assertThat(meterRegistry.get("opendata.publication.bucket.projected.ratio").gauge().value()).isGreaterThan(1);
    }

    @Test
    void failureInTheMiddleOfTheUploadLeavesThePublicVersionIntact() {
        storage.failUploadOf = ".staging/isbn-open-data.zip";

        assertThat(publisher.publish(datasets, globalManifest)).isFalse();

        assertOldVersionIntact();
        assertThat(storage.operations).noneMatch(operation -> operation.startsWith("copy") || operation.startsWith("put"));
        // The temporary objects of this pass are deleted; the leftover stays for the next publication.
        assertThat(storage.objects.keySet()).contains(".staging/leftover.zip")
                .doesNotContain(".staging/gtin-open-data.zip", ".staging/isbn-open-data.zip");
        assertFailed("public objects untouched");
    }

    @Test
    void localZipThatDoesNotMatchItsManifestIsRefused() throws IOException {
        Files.write(datasets.get(1).zipFile(), new byte[1]);

        assertThat(publisher.publish(datasets, globalManifest)).isFalse();

        assertThat(storage.operations).isEmpty();
        assertFailed("local zip");
    }

    @Test
    void disabledPublicationCreatesNoClient() {
        config.getPublication().setEnabled(false);

        assertThat(publisher.publish(datasets, globalManifest)).isFalse();

        assertThat(storagesCreated).hasValue(0);
        assertThat(publisher.status().state()).isEqualTo(State.IDLE);
        assertThat(meterRegistry.get("opendata.publication.enabled").gauge().value()).isZero();
    }

    @Test
    void missingCredentialsFailVisibly() {
        OpenDataPublisher real = new OpenDataPublisher(config, meterRegistry, S3OpenDataStorage::new);

        assertThat(real.publish(datasets, globalManifest)).isFalse();

        assertThat(real.status().state()).isEqualTo(State.FAILED);
        assertThat(real.status().error()).contains("R2_ACCOUNT_ID");
    }

    @Test
    void configurationNeverPrintsTheCredentials() {
        config.getPublication().setAccountId("0123456789abcdef0123456789abcdef");
        config.getPublication().setAccessKeyId("access-key-id-value");
        config.getPublication().setSecretAccessKey("secret-access-key-value");

        assertThat(config.getPublication().toString())
                .contains("https://0123456789abcdef0123456789abcdef.r2.cloudflarestorage.com")
                .doesNotContain("access-key-id-value", "secret-access-key-value");
    }

    @Test
    void carriedDatasetAlreadyServedByR2IsNotSentAgain() throws Exception {
        // GOU-2314: an impact dataset refused by the row guard keeps the version R2 already serves.
        OpenDataPublisher.Dataset oven = dataset("impact-oven", "impact-oven.zip", "impact-oven.zip", 1000);
        storage.objects.put("impact-oven.zip", new Stored(Files.readAllBytes(oven.zipFile()),
                new ObjectHeaders("application/zip", null, null, oven.manifest().sha256())));
        writeGlobalManifest(oven);

        assertThat(publisher.publish(datasets, List.of(oven), globalManifest)).isTrue();

        assertThat(storage.operations).contains("head impact-oven.zip", "put impact-oven-manifest.json")
                .doesNotContain("upload .staging/impact-oven.zip", "copy .staging/impact-oven.zip impact-oven.zip");
        assertThat(storage.operations.indexOf("put impact-oven-manifest.json"))
                .isLessThan(storage.operations.indexOf("put manifest.json"));
        assertThat(storage.objects.get("manifest.json").content()).isEqualTo(Files.readAllBytes(globalManifest));
    }

    @Test
    void carriedDatasetThatR2DoesNotServeIsSentWithTheNewOnes() throws Exception {
        // GOU-2314: the previous publication failed, R2 still serves an older zip than the local manifest announces.
        OpenDataPublisher.Dataset oven = dataset("impact-oven", "impact-oven.zip", "impact-oven.zip", 1000);
        storage.objects.put("impact-oven.zip", new Stored(OLD.getBytes(StandardCharsets.UTF_8),
                new ObjectHeaders("application/zip", null, null, sha256(OLD.getBytes(StandardCharsets.UTF_8)))));
        writeGlobalManifest(oven);

        assertThat(publisher.publish(datasets, List.of(oven), globalManifest)).isTrue();

        assertThat(storage.operations).contains("upload .staging/impact-oven.zip", "read .staging/impact-oven.zip",
                "copy .staging/impact-oven.zip impact-oven.zip", "put impact-oven-manifest.json");
        Stored zip = storage.objects.get("impact-oven.zip");
        assertThat(zip.content()).isEqualTo(Files.readAllBytes(oven.zipFile()));
        assertThat(zip.headers().sha256()).isEqualTo(oven.manifest().sha256());
        assertThat(storage.objects.get("manifest.json").content()).isEqualTo(Files.readAllBytes(globalManifest));
    }

    private void writeGlobalManifest(OpenDataPublisher.Dataset carried) throws IOException {
        List<DatasetManifest> listed = new java.util.ArrayList<>(datasets.stream().map(OpenDataPublisher.Dataset::manifest)
                .toList());
        listed.add(carried.manifest());
        new OpenDataManifestStore().write(new OpenDataManifest(OpenDataManifest.SCHEMA_VERSION, "2026-11-29T03:00:00Z",
                OpenDataManifest.LICENSE, listed), globalManifest.toFile());
        storage.operations.clear();
    }

    private void assertOldVersionIntact() {
        for (String key : List.of("gtin-open-data.zip", "isbn-open-data.zip", "gtin-manifest.json", "isbn-manifest.json",
                "manifest.json")) {
            assertThat(new String(storage.objects.get(key).content(), StandardCharsets.UTF_8)).as(key).isEqualTo(OLD);
        }
    }

    private void assertFailed(String error) {
        assertThat(publisher.status().state()).isEqualTo(State.FAILED);
        assertThat(publisher.status().error()).contains(error);
        assertThat(meterRegistry.get("opendata.publication.runs").tag("outcome", "failure").counter().count()).isEqualTo(1);
        assertThat(meterRegistry.get("opendata.publication.failed").gauge().value()).isEqualTo(1);
    }

    private OpenDataPublisher.Dataset dataset(String id, String localName, String publicName, int size)
            throws IOException {
        byte[] content = new byte[size];
        for (int i = 0; i < size; i++) {
            content[i] = (byte) (i * 31 + id.length());
        }
        Path zip = config.openDataDirectory().resolve(localName);
        Files.write(zip, content);
        DatasetManifest manifest = new DatasetManifest(OpenDataManifest.SCHEMA_VERSION, id, publicName,
                "https://data.nudger.fr/" + publicName, DatasetManifest.FORMAT, OpenDataManifest.LICENSE, size,
                sha256(content), 10, 10, "2026-11-22T02:55:00Z", List.of(new ColumnManifest(id, 1.0)), null);
        Path manifestFile = config.openDataDirectory().resolve(id + "-manifest.json");
        new OpenDataManifestStore().write(manifest, manifestFile.toFile());
        return new OpenDataPublisher.Dataset(manifest, zip, manifestFile);
    }

    private static String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
