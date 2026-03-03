package za.co.capitec.transactiondispute.disputes.interfaces.http;

import za.co.capitec.transactiondispute.disputes.domain.model.DisputeStatus;

public record DisputeDecisionRequest(
        DisputeStatus disputeStatus,
        String supportNote
) {}
