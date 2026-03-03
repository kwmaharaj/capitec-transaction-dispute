package za.co.capitec.transactiondispute.disputes.application.model;

import za.co.capitec.transactiondispute.disputes.domain.model.ReasonCode;

import java.util.UUID;

public record CreateDisputeCommand(
        UUID userId,
        UUID transactionId,
        ReasonCode reasonCode,
        String note
) {
}