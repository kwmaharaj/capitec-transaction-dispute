package za.co.capitec.transactiondispute.disputes.application.model;

import za.co.capitec.transactiondispute.disputes.domain.model.DisputeStatus;

import java.time.Instant;
import java.util.UUID;

public record CreateDisputeResult(
        UUID disputeId,
        UUID transactionId,
        DisputeStatus status,
        Instant createdAt
) {
}