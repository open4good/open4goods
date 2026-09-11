package org.open4goods.datareference.model.registry;

import java.io.IOException;
import java.io.InputStream;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Atomically publishes complete, Git-validated registry revisions to a read alias.
 *
 * <p>The importer accepts an entire resource, never a partial definition. It is
 * deliberately a deployment boundary rather than an administration API: callers
 * can inspect {@link #current()} but cannot add, remove, or edit a concept at
 * runtime. A changed registry must advance its semantic version; an exact hash
 * retry retains the installed snapshot and reports an idempotent import.
 */
public final class GitRegistryRuntimeImporter {

    /** Stable alias consumers use instead of a versioned physical index name. */
    public static final String READ_ALIAS = "o4g-registry-read";

    private final GitRegistryLoader loader;
    private final Clock clock;
    private final AtomicReference<RegistryRuntimeIndex> readAlias = new AtomicReference<>();

    /** Creates an importer using the checked-in loader and UTC system clock. */
    public GitRegistryRuntimeImporter() {
        this(new GitRegistryLoader(), Clock.systemUTC());
    }

    /**
     * Creates an importer with explicit collaborators for controlled deployment tests.
     *
     * @param loader loader that validates Git bytes before publication
     * @param clock clock used in immutable reconciliation evidence
     */
    public GitRegistryRuntimeImporter(GitRegistryLoader loader, Clock clock) {
        this.loader = Objects.requireNonNull(loader, "loader must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    /**
     * Validates and atomically installs the registry resource packaged with this module.
     *
     * @return import reconciliation evidence
     * @throws IOException when the checked-in resource cannot be read
     */
    public RegistryImportReport importDefault() throws IOException {
        return publish(loader.loadDefault());
    }

    /**
     * Validates and atomically installs one complete Git resource revision.
     *
     * @param resource exact bytes from the reviewed Git revision
     * @return import reconciliation evidence
     * @throws IOException when the resource cannot be read
     */
    public RegistryImportReport importGitResource(InputStream resource) throws IOException {
        return publish(loader.load(resource));
    }

    /**
     * Returns the immutable registry currently behind the read alias.
     *
     * @return installed registry, or empty before the first deployment import
     */
    public Optional<RegistryRuntimeIndex> current() {
        return Optional.ofNullable(readAlias.get());
    }

    private RegistryImportReport publish(RegistryRuntimeIndex candidate) {
        while (true) {
            RegistryRuntimeIndex installed = readAlias.get();
            if (installed != null && installed.contentHash().equals(candidate.contentHash())) {
                return report(installed, true);
            }
            if (installed != null && candidate.registry().version().value() <= installed.registry().version().value()) {
                throw new RegistryValidationException("registry content changed without a version increment: "
                        + candidate.registry().version());
            }
            if (readAlias.compareAndSet(installed, candidate)) {
                return report(candidate, false);
            }
        }
    }

    private RegistryImportReport report(RegistryRuntimeIndex index, boolean idempotent) {
        return new RegistryImportReport(READ_ALIAS, index.registry().version(), index.contentHash(), index.classCount(),
                index.attributeCount(), Instant.now(clock), idempotent);
    }
}
