package za.co.capitec.transactiondispute.disputes.application.exceptions;

import java.util.UUID;

public class OpenDisputeAlreadyExistsException extends RuntimeException {

    private final UUID transactionId;

    public OpenDisputeAlreadyExistsException(UUID transactionId) {
        super("Open dispute already exists for transaction: " + transactionId);
        this.transactionId = transactionId;
    }

    public UUID transactionId() {
        return transactionId;
    }
}
