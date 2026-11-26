package org.open4goods.services.opendata.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.open4goods.services.opendata.OpenDataPublicationException;
import org.open4goods.services.opendata.config.OpenDataConfig;
import org.open4goods.services.opendata.config.OpenDataConfig.Publication;
import org.open4goods.services.opendata.model.DatasetManifest;
import org.open4goods.services.opendata.model.ObjectHeaders;
import org.open4goods.services.opendata.model.OpenDataGenerationStatus.State;
import org.open4goods.services.opendata.model.OpenDataPublicationStatus;
import org.open4goods.services.opendata.model.StoredObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

/**
 * Publishes the zips and manifests of a successful generation pass on the R2 bucket (GOU-2289,
 * {@code open-data-config.publication}, disabled by default). The object keys are the same at each pass, so a zip is
 * never written to its public key directly:
 * <ol>
 * <li>budget guard: refused when the bucket would exceed {@code max-bucket-size} after the publication;</li>
 * <li>each zip is sent by multipart upload to {@code staging-prefix + key};</li>
 * <li>each temporary object is read back and its sha256 and size compared with the manifest;</li>
 * <li>each zip is copied server-side to its public key, then {@code gtin-manifest.json} and
 * {@code isbn-manifest.json}, then {@code manifest.json} last, the validation point;</li>
 * <li>only then are the temporary objects (of this pass and of an interrupted one) deleted.</li>
 * </ol>
 * A failure before step 4 leaves every public object untouched. Any failure is logged at ERROR, counted
 * ({@code opendata_publication_*}) and turns the open data health DOWN through {@link OpenDataService#health()}.
 */
@Service
public class OpenDataPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenDataPublisher.class);

    private static final Pattern SHA256 = Pattern.compile("^[0-9a-f]{64}$");
    private static final String ZIP_CONTENT_TYPE = "application/zip";
    private static final String CSV_CONTENT_TYPE = "text/csv; charset=utf-8";
    private static final String JSON_CONTENT_TYPE = "application/json";
    private static final String OUTCOME_TAG = "outcome";

    private final OpenDataConfig openDataConfig;
    private final MeterRegistry meterRegistry;
    private final Function<Publication, OpenDataStorage> storageFactory;

    private volatile OpenDataPublicationStatus status = OpenDataPublicationStatus.idle();

    /**
     * A dataset to publish: its manifest (already written locally), its local zip and its local manifest file.
     *
     * @param manifest     manifest of the dataset; {@code file} is the public key of the zip
     * @param zipFile      published local zip
     * @param manifestFile published local manifest of the dataset; its file name is its public key
     * @param sampleFile   published local CSV sample of the dataset (its file name is its public key), null when none
     */
    public record Dataset(DatasetManifest manifest, Path zipFile, Path manifestFile, Path sampleFile) {

        public Dataset(DatasetManifest manifest, Path zipFile, Path manifestFile) {
            this(manifest, zipFile, manifestFile, null);
        }
    }

    @Autowired
    public OpenDataPublisher(OpenDataConfig openDataConfig, ObjectProvider<MeterRegistry> meterRegistry) {
        this(openDataConfig, meterRegistry.getIfAvailable(SimpleMeterRegistry::new), S3OpenDataStorage::new);
    }

    OpenDataPublisher(OpenDataConfig openDataConfig, MeterRegistry meterRegistry,
            Function<Publication, OpenDataStorage> storageFactory) {
        Assert.notNull(openDataConfig, "openDataConfig must not be null");
        Assert.notNull(meterRegistry, "meterRegistry must not be null");
        Assert.notNull(storageFactory, "storageFactory must not be null");
        this.openDataConfig = openDataConfig;
        this.meterRegistry = meterRegistry;
        this.storageFactory = storageFactory;
        registerGauges();
        LOGGER.info("Open data publication is {}: {}", isEnabled() ? "enabled" : "disabled", publication());
    }

    private void registerGauges() {
        Gauge.builder("opendata.publication.enabled", this, publisher -> publisher.isEnabled() ? 1 : 0)
                .description("1 when the open data publication on R2 is enabled")
                .register(meterRegistry);
        Gauge.builder("opendata.publication.failed", this, publisher -> publisher.status.state() == State.FAILED ? 1 : 0)
                .description("1 when the last open data publication of this process failed")
                .register(meterRegistry);
        Gauge.builder("opendata.publication.last.success.timestamp", this, OpenDataPublisher::lastSuccessEpochSeconds)
                .description("End of the last successful open data publication of this process (NaN if none)")
                .baseUnit("seconds")
                .register(meterRegistry);
        Gauge.builder("opendata.publication.bucket.size", this, publisher -> publisher.status.bucketBytes() < 0
                        ? Double.NaN : publisher.status.bucketBytes())
                .description("Size of the open data bucket after the last successful publication (NaN if none)")
                .baseUnit("bytes")
                .register(meterRegistry);
    }

    private double lastSuccessEpochSeconds() {
        Instant lastSuccess = status.lastSuccessAt();
        return lastSuccess == null ? Double.NaN : lastSuccess.getEpochSecond();
    }

    private Publication publication() {
        return openDataConfig.getPublication();
    }

    /**
     * @return true when {@code open-data-config.publication.enabled} is set
     */
    public boolean isEnabled() {
        Publication publication = publication();
        return publication != null && publication.isEnabled();
    }

    /**
     * @return the publication state of this process
     */
    public OpenDataPublicationStatus status() {
        return status;
    }

    /**
     * Publishes the datasets and {@code globalManifest}. Never throws: a failure is logged at ERROR, counted and kept in
     * {@link #status()}.
     *
     * @return true when {@code manifest.json} was written and the temporary objects deleted; false when the publication
     *         is disabled or failed
     */
    public boolean publish(List<Dataset> datasets, Path globalManifest) {
        if (!isEnabled()) {
            LOGGER.info("Open data publication skipped: open-data-config.publication.enabled is false");
            return false;
        }
        Instant startedAt = Instant.now();
        OpenDataPublicationStatus previous = status;
        status = new OpenDataPublicationStatus(State.RUNNING, startedAt, null, previous.lastSuccessAt(), null,
                previous.bucketBytes());
        Timer.Sample sample = Timer.start(meterRegistry);
        String outcome = "failure";
        try (OpenDataStorage storage = storageFactory.apply(publication())) {
            long bucketBytes = publish(storage, datasets, globalManifest);
            Instant finishedAt = Instant.now();
            status = new OpenDataPublicationStatus(State.SUCCEEDED, startedAt, finishedAt, finishedAt, null, bucketBytes);
            outcome = "success";
            LOGGER.info("Open data publication succeeded in {}: bucket {} bytes", Duration.between(startedAt, finishedAt),
                    bucketBytes);
            return true;
        } catch (Exception e) {
            LOGGER.error("Open data publication on R2 failed", e);
            status = new OpenDataPublicationStatus(State.FAILED, startedAt, Instant.now(), previous.lastSuccessAt(),
                    e.getClass().getSimpleName() + ": " + e.getMessage(), previous.bucketBytes());
            return false;
        } finally {
            sample.stop(meterRegistry.timer("opendata.publication.duration", OUTCOME_TAG, outcome));
            meterRegistry.counter("opendata.publication.runs", OUTCOME_TAG, outcome).increment();
        }
    }

    /**
     * @return the size of the bucket after the publication
     */
    long publish(OpenDataStorage storage, List<Dataset> datasets, Path globalManifest)
            throws IOException, OpenDataPublicationException {
        Publication publication = publication();
        String stagingPrefix = publication.getStagingPrefix();
        if (StringUtils.isBlank(stagingPrefix) || !stagingPrefix.endsWith("/")) {
            throw new OpenDataPublicationException("open-data-config.publication.staging-prefix must end with /");
        }
        checkLocalFiles(datasets);
        byte[] global = Files.readAllBytes(globalManifest);
        List<byte[]> manifests = new ArrayList<>();
        for (Dataset dataset : datasets) {
            manifests.add(Files.readAllBytes(dataset.manifestFile()));
        }

        List<StoredObject> objects = storage.list("");
        checkBudget(objects, datasets, manifests, global, globalManifest, stagingPrefix,
                publication.getMaxBucketSize().toBytes());
        Set<String> toDelete = new LinkedHashSet<>();
        objects.stream().map(StoredObject::key).filter(key -> key.startsWith(stagingPrefix)).forEach(toDelete::add);

        List<String> staged = new ArrayList<>();
        try {
            for (Dataset dataset : datasets) {
                String stagingKey = stagingPrefix + dataset.manifest().file();
                LOGGER.info("Open data publication: sending {} ({} bytes) to {}", dataset.zipFile(),
                        dataset.manifest().bytes(), stagingKey);
                staged.add(stagingKey);
                storage.upload(dataset.zipFile(), stagingKey, zipHeaders(dataset.manifest()));
            }
            for (Dataset dataset : datasets) {
                checkReadBack(storage, stagingPrefix + dataset.manifest().file(), dataset.manifest());
            }
        } catch (OpenDataPublicationException e) {
            deleteQuietly(storage, staged);
            throw e;
        } catch (IOException | RuntimeException e) {
            deleteQuietly(storage, staged);
            throw new OpenDataPublicationException("Upload or read back failed, public objects untouched: "
                    + e.getMessage(), e);
        }

        // From here the public objects change: zips, per-dataset manifests, manifest.json last.
        try {
            for (Dataset dataset : datasets) {
                DatasetManifest manifest = dataset.manifest();
                storage.copy(stagingPrefix + manifest.file(), manifest.file(), manifest.bytes(), zipHeaders(manifest));
                checkPublicZip(storage, manifest);
            }
            for (Dataset dataset : datasets) {
                if (dataset.sampleFile() != null) {
                    storage.put(Files.readAllBytes(dataset.sampleFile()), dataset.sampleFile().getFileName().toString(),
                            csvHeaders());
                }
            }
            for (int i = 0; i < datasets.size(); i++) {
                storage.put(manifests.get(i), datasets.get(i).manifestFile().getFileName().toString(), jsonHeaders());
            }
            storage.put(global, globalManifest.getFileName().toString(), jsonHeaders());
        } catch (IOException | OpenDataPublicationException | RuntimeException e) {
            throw new OpenDataPublicationException("Switch to the new version failed: manifest.json not written, a zip "
                    + "may already be the new one until the next pass: " + e.getMessage(), e);
        }
        datasets.forEach(dataset -> LOGGER.info("Open data {} published on R2: {} rows, {} bytes, sha256 {}",
                dataset.manifest().id(), dataset.manifest().rows(), dataset.manifest().bytes(), dataset.manifest().sha256()));

        // Old objects are deleted only once manifest.json is written.
        toDelete.addAll(staged);
        try {
            for (String key : toDelete) {
                storage.delete(key);
            }
        } catch (IOException | RuntimeException e) {
            throw new OpenDataPublicationException("New version published, but the temporary objects " + toDelete
                    + " were not all deleted and count in the bucket: " + e.getMessage(), e);
        }
        return storage.list("").stream().mapToLong(StoredObject::bytes).sum();
    }

    private static void checkLocalFiles(List<Dataset> datasets) throws IOException, OpenDataPublicationException {
        if (datasets.isEmpty()) {
            throw new OpenDataPublicationException("No dataset to publish");
        }
        for (Dataset dataset : datasets) {
            DatasetManifest manifest = dataset.manifest();
            if (StringUtils.isBlank(manifest.file()) || manifest.file().contains("/")) {
                throw new OpenDataPublicationException("Dataset " + manifest.id() + ": invalid public file name '"
                        + manifest.file() + "'");
            }
            if (manifest.sha256() == null || !SHA256.matcher(manifest.sha256()).matches()) {
                throw new OpenDataPublicationException("Dataset " + manifest.id() + ": invalid sha256 in the manifest");
            }
            long size = Files.size(dataset.zipFile());
            if (size <= 0 || size != manifest.bytes()) {
                throw new OpenDataPublicationException(String.format(Locale.ROOT,
                        "Dataset %s: local zip %s has %d bytes, the manifest %d", manifest.id(), dataset.zipFile(), size,
                        manifest.bytes()));
            }
        }
    }

    /**
     * Budget guard: size of the bucket once this publication is done, that is the objects kept (neither replaced nor
     * temporary) plus the new zips and manifests. During the upload the temporary copies also count, for a few hours.
     */
    private static void checkBudget(List<StoredObject> objects, List<Dataset> datasets, List<byte[]> manifests,
            byte[] global, Path globalManifest, String stagingPrefix, long maxBytes) throws OpenDataPublicationException {
        Set<String> replaced = new LinkedHashSet<>();
        long added = global.length;
        replaced.add(globalManifest.getFileName().toString());
        for (int i = 0; i < datasets.size(); i++) {
            replaced.add(datasets.get(i).manifest().file());
            replaced.add(datasets.get(i).manifestFile().getFileName().toString());
            added += datasets.get(i).manifest().bytes() + manifests.get(i).length;
        }
        long current = objects.stream().mapToLong(StoredObject::bytes).sum();
        long kept = objects.stream()
                .filter(object -> !object.key().startsWith(stagingPrefix) && !replaced.contains(object.key()))
                .mapToLong(StoredObject::bytes)
                .sum();
        long projected = kept + added;
        if (projected > maxBytes) {
            throw new OpenDataPublicationException(String.format(Locale.ROOT,
                    "Budget guard: the bucket would hold %d bytes after the publication, above %d "
                            + "(open-data-config.publication.max-bucket-size); %d bytes now, nothing sent",
                    projected, maxBytes, current));
        }
        LOGGER.info("Open data publication: bucket {} bytes now, {} after the publication (maximum {})", current,
                projected, maxBytes);
    }

    /** Reads the temporary object back and compares its sha256 and size with the manifest. */
    private static void checkReadBack(OpenDataStorage storage, String stagingKey, DatasetManifest manifest)
            throws IOException, OpenDataPublicationException {
        MessageDigest digest = sha256Digest();
        long bytes = 0;
        try (InputStream in = new DigestInputStream(storage.read(stagingKey), digest)) {
            byte[] buffer = new byte[64 * 1024];
            int read;
            while ((read = in.read(buffer)) >= 0) {
                bytes += read;
            }
        }
        String remote = HexFormat.of().formatHex(digest.digest());
        if (!remote.equals(manifest.sha256()) || bytes != manifest.bytes()) {
            throw new OpenDataPublicationException(String.format(Locale.ROOT,
                    "Dataset %s: %s read back has sha256 %s and %d bytes, the manifest %s and %d; public objects untouched",
                    manifest.id(), stagingKey, remote, bytes, manifest.sha256(), manifest.bytes()));
        }
        LOGGER.info("Open data publication: {} read back, sha256 {} matches the manifest", stagingKey, remote);
    }

    private static void checkPublicZip(OpenDataStorage storage, DatasetManifest manifest)
            throws IOException, OpenDataPublicationException {
        Optional<StoredObject> object = storage.head(manifest.file());
        if (object.isEmpty() || object.get().bytes() != manifest.bytes()
                || !manifest.sha256().equals(object.get().sha256())) {
            throw new OpenDataPublicationException("Dataset " + manifest.id() + ": public object " + manifest.file()
                    + " after the copy is " + object.orElse(null) + ", expected " + manifest.bytes() + " bytes and sha256 "
                    + manifest.sha256() + "; manifests not written");
        }
    }

    private ObjectHeaders zipHeaders(DatasetManifest manifest) {
        return new ObjectHeaders(ZIP_CONTENT_TYPE, "attachment; filename=\"" + manifest.file() + "\"",
                StringUtils.trimToNull(publication().getZipCacheControl()), manifest.sha256());
    }

    private ObjectHeaders csvHeaders() {
        return new ObjectHeaders(CSV_CONTENT_TYPE, null, StringUtils.trimToNull(publication().getManifestCacheControl()),
                null);
    }

    private ObjectHeaders jsonHeaders() {
        return new ObjectHeaders(JSON_CONTENT_TYPE, null, StringUtils.trimToNull(publication().getManifestCacheControl()),
                null);
    }

    private static void deleteQuietly(OpenDataStorage storage, List<String> keys) {
        for (String key : keys) {
            try {
                storage.delete(key);
            } catch (IOException | RuntimeException e) {
                LOGGER.warn("Unable to delete the temporary object {}, the next publication deletes it: {}", key,
                        e.getMessage());
            }
        }
    }

    private static MessageDigest sha256Digest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
