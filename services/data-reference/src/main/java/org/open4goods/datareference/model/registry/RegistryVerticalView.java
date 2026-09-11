package org.open4goods.datareference.model.registry;

import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

import org.open4goods.datareference.model.CanonicalClassId;

/**
 * An editorial product vertical expressed as an explicit view over O4G classes.
 *
 * <p>A vertical is presentation and navigation metadata, not a canonical class
 * identifier. Its included classes stay explicit so a newly introduced class
 * cannot enter a public view merely because a provider hierarchy happened to
 * use a similar label.
 *
 * @param verticalId stable legacy/editorial vertical identifier
 * @param includedClasses explicitly curated O4G classes in this view
 */
public record RegistryVerticalView(String verticalId, List<CanonicalClassId> includedClasses) {

    private static final Pattern VERTICAL_ID = Pattern.compile("[a-z0-9]+(?:-[a-z0-9]+)*");

    /** Validates a non-empty, duplicate-free editorial class list. */
    public RegistryVerticalView {
        Objects.requireNonNull(verticalId, "verticalId must not be null");
        if (!VERTICAL_ID.matcher(verticalId).matches()) {
            throw new IllegalArgumentException("verticalId must be lower-case kebab-case: " + verticalId);
        }
        includedClasses = List.copyOf(Objects.requireNonNull(includedClasses, "includedClasses must not be null"));
        if (includedClasses.isEmpty()) {
            throw new IllegalArgumentException("a vertical view must include at least one O4G class: " + verticalId);
        }
        if (includedClasses.stream().distinct().count() != includedClasses.size()) {
            throw new IllegalArgumentException("a vertical view cannot include a class twice: " + verticalId);
        }
    }
}
