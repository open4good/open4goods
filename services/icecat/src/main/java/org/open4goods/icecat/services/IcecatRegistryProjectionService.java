package org.open4goods.icecat.services;

import java.io.IOException;
import java.util.Objects;

import org.open4goods.datareference.model.registry.GitRegistryRuntimeImporter;
import org.open4goods.datareference.model.registry.RegistryImportReport;
import org.open4goods.datareference.model.registry.RegistryRuntimeIndex;
import org.springframework.stereotype.Service;

/**
 * Owns the in-process read alias for complete, Git-authored registry projections.
 *
 * <p>A candidate is validated before it replaces the current alias. Consequently a
 * failed rebuild preserves the prior complete projection; no HTTP endpoint can edit
 * an individual mapping.
 */
@Service
public class IcecatRegistryProjectionService {

    private final GitRegistryRuntimeImporter importer;

    public IcecatRegistryProjectionService() {
        this(new GitRegistryRuntimeImporter());
    }

    IcecatRegistryProjectionService(GitRegistryRuntimeImporter importer) {
        this.importer = Objects.requireNonNull(importer, "importer must not be null");
    }

    /** Returns the installed projection, importing the packaged Git revision once when needed. */
    public RegistryRuntimeIndex current() {
        if (importer.current().isEmpty()) {
            importDefault();
        }
        return importer.current().orElseThrow(() -> new IllegalStateException("registry projection was not installed"));
    }

    /**
     * Rebuilds from the packaged Git revision only when the operator's expected hash is current.
     *
     * @param expectedHash the currently observed registry SHA-256, or {@code null}
     * @return evidence for the atomically installed (or retained) projection
     */
    public RegistryImportReport rebuild(String expectedHash) {
        RegistryRuntimeIndex installed = current();
        if (expectedHash != null && !expectedHash.equals(installed.contentHash())) {
            throw new StaleRegistryProjectionException(expectedHash, installed.contentHash());
        }
        return importDefault();
    }

    private RegistryImportReport importDefault() {
        try {
            return importer.importDefault();
        } catch (IOException exception) {
            throw new IllegalStateException("cannot load the Git-authored O4G registry", exception);
        }
    }
}
