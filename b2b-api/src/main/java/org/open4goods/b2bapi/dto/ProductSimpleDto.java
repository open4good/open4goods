package org.open4goods.b2bapi.dto;

public record ProductSimpleDto(
        long gtin,
        String vertical,
        int offersCount
) {}
