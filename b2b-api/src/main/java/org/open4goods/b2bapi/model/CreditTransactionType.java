package org.open4goods.b2bapi.model;

/**
 * Append-only credit ledger transaction type.
 */
public enum CreditTransactionType {
    GRANT,
    DEBIT,
    REFUND,
    EXPIRE,
    ADJUST
}
