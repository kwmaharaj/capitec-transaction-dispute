package za.co.capitec.transactiondispute.disputes.application.exceptions;

import java.util.UUID;

public class TransactionNotFoundException extends RuntimeException {

    private final UUID transactionId;

    public TransactionNotFoundException(UUID transactionId) {
        super("Transaction not found: " + transactionId);
        this.transactionId = transactionId;
    }

    public UUID transactionId() {
        return transactionId;
    }
}
