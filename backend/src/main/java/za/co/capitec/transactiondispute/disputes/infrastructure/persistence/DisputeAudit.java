package za.co.capitec.transactiondispute.disputes.infrastructure.persistence;

import java.time.Instant;

public record DisputeAudit(Instant createdAt, Instant updatedAt) {}