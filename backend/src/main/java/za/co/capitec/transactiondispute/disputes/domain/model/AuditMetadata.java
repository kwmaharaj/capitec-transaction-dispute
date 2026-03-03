package za.co.capitec.transactiondispute.disputes.domain.model;

import java.time.Instant;
import java.util.Objects;

/**
 * limit constructor field limit in Disputes- max 7
 */
public record AuditMetadata(Instant createdAt, Instant updatedAt) {

    public AuditMetadata {
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(updatedAt, "updatedAt");
    }
}
