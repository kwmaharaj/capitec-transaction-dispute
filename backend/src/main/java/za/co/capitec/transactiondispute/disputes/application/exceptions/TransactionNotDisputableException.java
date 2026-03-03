package za.co.capitec.transactiondispute.disputes.application.exceptions;

import java.util.UUID;

public class TransactionNotDisputableException extends RuntimeException {

    private final UUID transactionId;
    private final String status;

    public TransactionNotDisputableException(UUID transactionId, String status) {
        super("Transaction status is not disputable");
        this.transactionId = transactionId;
        this.status = status;
    }

    public UUID transactionId() { return transactionId; }
    public String status() { return status; }
}