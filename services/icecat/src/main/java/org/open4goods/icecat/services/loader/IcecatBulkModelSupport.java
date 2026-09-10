package org.open4goods.icecat.services.loader;

import java.math.BigInteger;

import org.open4goods.icecat.jaxb.Name;
import org.open4goods.icecat.jaxb.Supplier;

/**
 * Conversion helpers for the JAXB-generated Icecat bulk-XML contract, bridging its
 * {@link BigInteger} attributes and dual attribute-or-text-content conventions to the plain
 * values the rest of the codebase uses.
 */
public final class IcecatBulkModelSupport {

    private IcecatBulkModelSupport() {
    }

    public static Integer intValue(BigInteger value) {
        return value != null ? value.intValue() : null;
    }

    public static int intValue(BigInteger value, int defaultValue) {
        return value != null ? value.intValue() : defaultValue;
    }

    /**
     * Returns the effective display name regardless of which Icecat XML representation is used:
     * prefers the {@code Value} attribute, falls back to the element's text content.
     */
    public static String effectiveName(Name name) {
        return name.getValueAttribute() != null ? name.getValueAttribute() : name.getValue();
    }

    /**
     * Returns the brand name, resolving Icecat's inconsistent casing of the
     * {@code Name} / {@code name} attribute.
     */
    public static String effectiveName(Supplier supplier) {
        return supplier.getName() != null ? supplier.getName() : supplier.getNameLowercase();
    }

    /** Returns the best available logo URL (prefers high-res, falls back to medium, low, standard). */
    public static String bestLogoUrl(Supplier supplier) {
        if (supplier.getLogoHighPic() != null) {
            return supplier.getLogoHighPic();
        }
        if (supplier.getLogoMediumPic() != null) {
            return supplier.getLogoMediumPic();
        }
        if (supplier.getLogoLowPic() != null) {
            return supplier.getLogoLowPic();
        }
        return supplier.getLogoPic();
    }
}
