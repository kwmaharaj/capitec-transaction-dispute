package za.co.capitec.transactiondispute.disputes.application.exceptions;

import java.time.Instant;
import java.util.UUID;

public class TransactionDisputeWindowExpiredException extends RuntimeException {

    private final UUID transactionId;
    private final Instant postedAt;
    private final int maxAgeDays;

    public TransactionDisputeWindowExpiredException(UUID transactionId, Instant postedAt, int maxAgeDays) {
        super("Transaction is outside the dispute window");
        this.transactionId = transactionId;
        this.postedAt = postedAt;
        this.maxAgeDays = maxAgeDays;
    }

    public UUID transactionId() { return transactionId; }
    public Instant postedAt() { return postedAt; }
    public int maxAgeDays() { return maxAgeDays; }
}