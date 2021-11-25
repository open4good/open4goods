package org.open4goods.model.product;

import java.util.HashSet;
import java.util.Set;

import org.apache.lucene.util.AttributeSource;

public class AttributeConflictReport {
	public enum AttributeConflictStatus {
		NO_CONFLICT,
		UNSOLVED_CONFLICT,
		MAJORITY_SOLVED_CONFLICT,
		REFERENTIEL_DATA_SOLVED_CONFLICT
	}

	private AttributeConflictStatus conflictStatus;

	private Integer numberOfSources;

	/**
	 * The number of sources that have the same value for this attribute
	 */
	private Integer conforms;

	/**
	 * The number of sources that have the same value that the elected one
	 */
	private Integer differences;

	/**
	 * The list of other values (the differences attribute sources)
	 */
	private Set<AttributeSource> differencesSources = new HashSet<>();

	public AttributeConflictStatus getConflictStatus() {
		return conflictStatus;
	}

	public void setConflictStatus(final AttributeConflictStatus conflictStatus) {
		this.conflictStatus = conflictStatus;
	}

	public Integer getNumberOfSources() {
		return numberOfSources;
	}

	public void setNumberOfSources(final Integer numberOfSources) {
		this.numberOfSources = numberOfSources;
	}

	public Integer getConforms() {
		return conforms;
	}

	public void setConforms(final Integer equalities) {
		conforms = equalities;
	}

	public Integer getDifferences() {
		return differences;
	}

	public void setDifferences(final Integer differences) {
		this.differences = differences;
	}

	public Set<AttributeSource> getDifferencesSources() {
		return differencesSources;
	}

	public void setDifferencesSources(final Set<AttributeSource> differencesSources) {
		this.differencesSources = differencesSources;
	}




}
