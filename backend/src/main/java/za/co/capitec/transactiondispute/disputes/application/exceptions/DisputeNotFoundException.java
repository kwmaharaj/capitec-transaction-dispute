package za.co.capitec.transactiondispute.disputes.application.exceptions;

import java.util.UUID;

public class DisputeNotFoundException extends RuntimeException {

    private final UUID disputeId;

    public DisputeNotFoundException(UUID disputeId) {
        super("Dispute not found: " + disputeId);
        this.disputeId = disputeId;
    }

    public UUID disputeId() {
        return disputeId;
    }
}
