package za.co.capitec.transactiondispute.disputes.interfaces.http;

import jakarta.validation.constraints.NotNull;
import za.co.capitec.transactiondispute.disputes.domain.model.ReasonCode;

import java.util.UUID;

public record CreateDisputeRequest(
        @NotNull UUID transactionId,
        @NotNull ReasonCode reasonCode,
        String note
) {
}
