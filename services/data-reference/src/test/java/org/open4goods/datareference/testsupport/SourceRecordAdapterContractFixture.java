package org.open4goods.datareference.testsupport;

import java.time.Instant;
import java.util.Optional;

import org.open4goods.datareference.model.SourceRecordMutation;

/**
 * Row fixtures and adapter entry points a concrete source module supplies to
 * {@link SourceRecordAdapterContractTest}.
 *
 * <p>Each method names the one property of its row that matters to the scenario it feeds; a
 * fixture implementation is free to reuse the same base row across several methods as long as
 * that property holds.
 *
 * @param <T> provider row type consumed by the adapter under test
 */
public interface SourceRecordAdapterContractFixture<T> {

    /**
     * Runs the adapter under test, requiring a mutation.
     *
     * @param row provider row
     * @param schemaVersion O4G contract version to adapt against
     * @param retrievedAt instant this row was retrieved
     * @return the produced mutation
     * @throws AssertionError when the adapter rejects the row
     */
    SourceRecordMutation adapt(T row, String schemaVersion, Instant retrievedAt);

    /**
     * Runs the adapter under test without requiring a mutation, for rejection scenarios.
     *
     * @param row provider row
     * @param schemaVersion O4G contract version to adapt against
     * @param retrievedAt instant this row was retrieved
     * @return the adapter's result, possibly empty
     */
    Optional<SourceRecordMutation> attempt(T row, String schemaVersion, Instant retrievedAt);

    /** A minimal, valid, deterministic row: adapting it twice must produce equal output. */
    T repeatableRow();

    /** A row carrying at least one assertion with an unrecognized unit or language tag. */
    T rowWithUnknownUnitOrLanguage();

    /** A row observed before a provider-version-only change. */
    T rowBeforeProviderVersionChange();

    /** The same record as {@link #rowBeforeProviderVersionChange()}, with only its provider version changed. */
    T rowAfterProviderVersionChange();

    /** A row observed before its GTIN attachment is corrected. */
    T rowBeforeGtinCorrection();

    /** The same record as {@link #rowBeforeGtinCorrection()}, with a corrected GTIN attachment. */
    T rowAfterGtinCorrection();

    /** A row the provider marks as withdrawn. */
    T withdrawnRow();

    /** A row carrying no stable identifier the adapter can key a record on. */
    T rowWithoutIdentifier();
}
