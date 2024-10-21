package org.open4goods.commons.model.dto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.open4goods.commons.model.constants.ProductCondition;

public class VerticalSearchRequest {

    Integer pageNumber;
    Integer pageSize;

    Set<String> countries = new HashSet<>();

    List<NumericRangeFilter> numericFilters = new ArrayList<>();

    Map<String, Set<String>> termsFilter = new HashMap<>();

    boolean excluded = false;

    private String sortField;
    private String sortOrder;

    public void addTermFilter(String attribute, String term) {
        if (!termsFilter.containsKey(attribute)) {
            termsFilter.put(attribute, new HashSet<>());
        }
        termsFilter.get(attribute).add(term);
    }


    public Integer getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(Integer from) {
        pageNumber = from;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer to) {
        pageSize = to;
    }

    public Set<String> getCountries() {
        return countries;
    }

    public void setCountries(Set<String> countries) {
        this.countries = countries;
    }

    public String getSortField() {
        return sortField;
    }

    public void setSortField(String sortField) {
        this.sortField = sortField;
    }

    public String getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(String sortOrder) {
        this.sortOrder = sortOrder;
    }

    public List<NumericRangeFilter> getNumericFilters() {
        return numericFilters;
    }

    public void setNumericFilters(List<NumericRangeFilter> numericFilters) {
        this.numericFilters = numericFilters;
    }

    public Map<String, Set<String>> getTermsFilter() {
        return termsFilter;
    }

    public void setTermsFilter(Map<String, Set<String>> termsFilter) {
        this.termsFilter = termsFilter;
    }

    public boolean isExcluded() {
        return excluded;
    }

    public void setExcluded(boolean excluded) {
        this.excluded = excluded;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        VerticalSearchRequest that = (VerticalSearchRequest) obj;

        return new EqualsBuilder()
            .append(excluded, that.excluded)
            .append(pageNumber, that.pageNumber)
            .append(pageSize, that.pageSize)
            .append(countries, that.countries)
            .append(numericFilters, that.numericFilters)
            .append(termsFilter, that.termsFilter)
            .append(sortField, that.sortField)
            .append(sortOrder, that.sortOrder)
            .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
            .append(pageNumber)
            .append(pageSize)
            .append(countries)
            .append(numericFilters)
            .append(termsFilter)
            .append(excluded)
            .append(sortField)
            .append(sortOrder)
            .toHashCode();
    }
    
    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
            .append("pageNumber", pageNumber)
            .append("pageSize", pageSize)
            .append("countries", countries)
            .append("numericFilters", numericFilters)
            .append("termsFilter", termsFilter)
            .append("excluded", excluded)
            .append("sortField", sortField)
            .append("sortOrder", sortOrder)
            .toString();
    }
    
    
}
