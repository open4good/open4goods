package org.open4goods.icecat.model;

/** An indexed Icecat category with no approved mapping in the O4G registry. */
public record IcecatUnmappedCategory(Integer id, String name, Integer parentId, Integer score) {
}
