package za.co.capitec.transactiondispute.disputes.application.model;

import za.co.capitec.transactiondispute.disputes.domain.model.DisputeStatus;
import za.co.capitec.transactiondispute.disputes.domain.model.TransactionStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record DisputeSearchCriteria(
        UUID disputeId,
        BigDecimal minAmount,
        BigDecimal maxAmount,
        String rrn,
        String merchant,
        DisputeStatus disputeStatus,
        TransactionStatus transactionStatus,
        Instant fromDate,
        Instant toDate
) {
    public static DisputeSearchCriteria empty() {
        return new DisputeSearchCriteria(null, null, null, null, null, null, null, null, null);
    }
}
