package za.co.capitec.transactiondispute.transactions.domain.model;

/**
 * Local copy of DisputeStatus for the transactions module.
 * This avoids a compile-time dependency from transactions -> disputes while still keeping
 * status strongly typed across layers.
 */
public enum DisputeStatus {
    OPEN,
    IN_PROGRESS,
    RESOLVED,
    REJECTED
}
