package za.co.capitec.transactiondispute.disputes.domain.model;

/**
 * Local copy of TransactionStatus for the disputes module.
 * This avoids a compile-time dependency from disputes -> transactions while still keeping
 * status strongly typed across layers.
 */
public enum TransactionStatus {
    POSTED,
    PENDING,
    REVERSED,
    SETTLED
}
