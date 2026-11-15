package org.open4goods.services.opendata.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
import org.open4goods.services.opendata.model.StoredObject;
import org.springframework.util.unit.DataSize;

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
    private InMemoryStorage storage;
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
        storage = new InMemoryStorage();
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
                sha256(content), 10, 10, "2026-11-22T02:55:00Z", List.of(new ColumnManifest(id, 1.0)));
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

    private record Stored(byte[] content, ObjectHeaders headers) {
    }

    /** A bucket in memory, recording each call. */
    private static final class InMemoryStorage implements OpenDataStorage {

        final Map<String, Stored> objects = new LinkedHashMap<>();
        final List<String> operations = new ArrayList<>();
        boolean corruptUploads;
        String failUploadOf;

        long totalBytes() {
            return objects.values().stream().mapToLong(stored -> stored.content().length).sum();
        }

        @Override
        public List<StoredObject> list(String prefix) {
            operations.add("list " + prefix);
            return objects.entrySet().stream().filter(entry -> entry.getKey().startsWith(prefix))
                    .map(entry -> new StoredObject(entry.getKey(), entry.getValue().content().length, null))
                    .toList();
        }

        @Override
        public Optional<StoredObject> head(String key) {
            operations.add("head " + key);
            Stored stored = objects.get(key);
            return stored == null ? Optional.empty() : Optional.of(new StoredObject(key, stored.content().length,
                    stored.headers() == null ? null : stored.headers().sha256()));
        }

        @Override
        public void upload(Path file, String key, ObjectHeaders headers) throws IOException {
            operations.add("upload " + key);
            if (key.equals(failUploadOf)) {
                throw new IOException("connection reset during part 2");
            }
            byte[] content = Files.readAllBytes(file);
            if (corruptUploads) {
                content[0] ^= 1;
            }
            objects.put(key, new Stored(content, headers));
        }

        @Override
        public void put(byte[] content, String key, ObjectHeaders headers) {
            operations.add("put " + key);
            objects.put(key, new Stored(content.clone(), headers));
        }

        @Override
        public InputStream read(String key) throws IOException {
            operations.add("read " + key);
            Stored stored = objects.get(key);
            if (stored == null) {
                throw new IOException("NoSuchKey " + key);
            }
            return new ByteArrayInputStream(stored.content());
        }

        @Override
        public void copy(String sourceKey, String targetKey, long bytes, ObjectHeaders headers) {
            operations.add("copy " + sourceKey + " " + targetKey);
            objects.put(targetKey, new Stored(objects.get(sourceKey).content(), headers));
        }

        @Override
        public void delete(String key) {
            operations.add("delete " + key);
            objects.remove(key);
        }

        @Override
        public void close() {
            // Nothing to release.
        }
    }
}
