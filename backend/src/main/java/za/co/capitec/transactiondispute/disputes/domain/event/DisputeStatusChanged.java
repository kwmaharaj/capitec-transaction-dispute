package za.co.capitec.transactiondispute.disputes.domain.event;

import za.co.capitec.transactiondispute.disputes.domain.model.DisputeStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain event raised when a dispute status changes. open -> inprogress......
 */
public record DisputeStatusChanged(
        UUID disputeId,
        UUID transactionId,
        UUID userId,
        UUID decidedBy,
        DisputeStatus previousStatus,
        DisputeStatus newStatus,
        String note,
        Instant occurredAt
) {}
