package org.open4goods.eprelservice.model;

import java.util.Optional;

/**
 * Utility methods around GTIN handling.
 */
public final class GtinHelper
{
    private GtinHelper()
    {
    }

    /**
     * Converts a textual GTIN into its numeric representation.
     *
     * @param gtin input value
     * @return optional numeric representation
     */
    public static Optional<Long> toNumeric(String gtin)
    {
        if (gtin == null)
        {
            return Optional.empty();
        }
        String digits = gtin.trim();
        if (digits.isEmpty())
        {
            return Optional.empty();
        }
        if (!digits.chars().allMatch(Character::isDigit))
        {
            return Optional.empty();
        }
        try
        {
            return Optional.of(Long.parseLong(digits));
        }
        catch (NumberFormatException e)
        {
            return Optional.empty();
        }
    }
}
