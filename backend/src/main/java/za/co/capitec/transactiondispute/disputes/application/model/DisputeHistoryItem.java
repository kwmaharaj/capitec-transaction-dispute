package za.co.capitec.transactiondispute.disputes.application.model;

import za.co.capitec.transactiondispute.disputes.domain.model.DisputeStatus;
import za.co.capitec.transactiondispute.disputes.infrastructure.persistence.DisputeActor;

import java.time.Instant;
import java.util.UUID;

public record DisputeHistoryItem(
        UUID historyId,
        UUID disputeId,
        DisputeStatus fromDisputeStatus,
        DisputeStatus toDisputeStatus,
        String note,
        DisputeActor disputeActor,
        Instant changedAt
) {}
