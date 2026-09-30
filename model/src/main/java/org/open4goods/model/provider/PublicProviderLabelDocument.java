package org.open4goods.model.provider;

import java.util.Collections;
import java.util.List;

/**
 * Git-versioned document backing {@link PublicProviderLabelRegistry}.
 *
 * @param labels reviewed public provider labels
 */
public record PublicProviderLabelDocument(List<PublicProviderLabel> labels) {

    public PublicProviderLabelDocument {
        labels = labels == null ? Collections.emptyList() : List.copyOf(labels);
    }
}
