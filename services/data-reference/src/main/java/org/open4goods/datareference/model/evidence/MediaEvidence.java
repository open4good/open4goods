package org.open4goods.datareference.model.evidence;

import java.net.URI;
import java.util.Objects;

import org.open4goods.datareference.model.LanguageTag;

/**
 * A provider reference to an image, document or other binary.
 *
 * <p>The binary itself is never embedded: media caching is governed by usage
 * policy, and a contract that carried bytes would make every head a licensing
 * decision. Images are media evidence, never scalar evidence holding a URL as
 * text, so that policy filtering can find them by shape.
 *
 * @param uri provider location of the media
 * @param mediaType IANA media type stated by the provider, or {@code null}
 * @param role provider role such as a gallery position, or {@code null}
 * @param language language of any embedded text, {@code und} when none
 */
public record MediaEvidence(URI uri, String mediaType, String role, LanguageTag language)
        implements SourceEvidence {

    /**
     * Validates the media reference.
     */
    public MediaEvidence {
        Objects.requireNonNull(uri, "uri must not be null");
        Objects.requireNonNull(language, "language must not be null");
        if (!uri.isAbsolute()) {
            throw new IllegalArgumentException("media uri must be absolute: " + uri);
        }
        if (mediaType != null && mediaType.isBlank()) {
            throw new IllegalArgumentException("mediaType must be absent rather than blank");
        }
        if (role != null && role.isBlank()) {
            throw new IllegalArgumentException("role must be absent rather than blank");
        }
    }

    @Override
    public SourceEvidenceKind kind() {
        return SourceEvidenceKind.MEDIA;
    }
}
