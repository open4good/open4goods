package org.open4goods.model.eprel;

import java.util.List;
import java.util.Objects;

/**
 * One commercial model of a brand and the identifiers that name it, used to find the model under which the
 * manufacturer registered it in EPREL.
 *
 * <p>Example (Apple): EPREL model {@code A3094}, other regional numbers {@code A2847}, {@code A3093},
 * {@code A3096}, commercial name "iPhone 15 Plus", part-number bases {@code MU163}, {@code MU0Y3}.
 *
 * @param eprelModel  model identifier registered in EPREL (for Apple, the number for "other countries and
 *                    regions", which covers the EU)
 * @param otherModels other regulatory numbers of the same commercial model (other regions), never registered in
 *                    EPREL but sometimes given by feeds
 * @param names       commercial names, matched as whole phrases in offer titles
 * @param mpnBases    manufacturer part-number bases (the part number without its region suffix and {@code /A}),
 *                    one per colour and capacity
 */
public record EprelModelAlias(String eprelModel, List<String> otherModels, List<String> names, List<String> mpnBases)
{
    /**
     * Normalises absent lists to empty lists and drops null entries.
     */
    public EprelModelAlias
    {
        otherModels = clean(otherModels);
        names = clean(names);
        mpnBases = clean(mpnBases);
    }

    private static List<String> clean(List<String> values)
    {
        return values == null ? List.of() : values.stream().filter(Objects::nonNull).toList();
    }
}
