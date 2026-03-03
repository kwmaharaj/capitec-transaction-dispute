package za.co.capitec.transactiondispute.disputes.application.exceptions;

import java.util.UUID;

public class TransactionNotOwnedException extends RuntimeException {

    private final UUID transactionId;
    private final UUID userId;

    public TransactionNotOwnedException(UUID transactionId, UUID userId) {
        super("Transaction not accessible for user");
        this.transactionId = transactionId;
        this.userId = userId;
    }

    public UUID transactionId() { return transactionId; }
    public UUID userId() { return userId; }
}