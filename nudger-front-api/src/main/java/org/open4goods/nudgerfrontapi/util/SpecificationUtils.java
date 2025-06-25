package org.open4goods.nudgerfrontapi.util;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.open4goods.nudgerfrontapi.dto.filter.FilterCriteria;
import org.open4goods.nudgerfrontapi.dto.filter.FilterOperator;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;

/**
 * Build JPA {@link Specification} objects from {@link FilterCriteria} lists.
 */
public final class SpecificationUtils {

    private SpecificationUtils() {
    }

    /**
     * Convert a list of filter criteria to a JPA {@link Specification}.
     *
     * @param filters criteria to convert
     * @return combined specification applying all filters with AND semantics
     */
    public static <T> Specification<T> fromFilters(List<FilterCriteria> filters) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            for (FilterCriteria fc : filters) {
                Path<?> path = root.get(fc.field());
                Object value = convertValue(fc.value(), path.getJavaType());
                switch (fc.operator()) {
                    case EQ -> predicates.add(cb.equal(path, value));
                    case NE -> predicates.add(cb.notEqual(path, value));
                    case GT -> predicates.add(cb.greaterThan(path.as(Comparable.class), (Comparable) value));
                    case GTE -> predicates.add(cb.greaterThanOrEqualTo(path.as(Comparable.class), (Comparable) value));
                    case LT -> predicates.add(cb.lessThan(path.as(Comparable.class), (Comparable) value));
                    case LTE -> predicates.add(cb.lessThanOrEqualTo(path.as(Comparable.class), (Comparable) value));
                    case LIKE -> predicates.add(cb.like(path.as(String.class), "%" + value + "%"));
                }
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static Object convertValue(String raw, Class<?> type) {
        if (raw == null) {
            return null;
        }
        if (Long.class.isAssignableFrom(type) || long.class.isAssignableFrom(type)) {
            return Long.valueOf(raw);
        }
        if (Integer.class.isAssignableFrom(type) || int.class.isAssignableFrom(type)) {
            return Integer.valueOf(raw);
        }
        if (Double.class.isAssignableFrom(type) || double.class.isAssignableFrom(type)) {
            return Double.valueOf(raw);
        }
        if (BigDecimal.class.isAssignableFrom(type)) {
            return new BigDecimal(raw);
        }
        if (Boolean.class.isAssignableFrom(type) || boolean.class.isAssignableFrom(type)) {
            return Boolean.valueOf(raw);
        }
        return raw;
    }
}
