package za.co.capitec.transactiondispute.disputes.application.model;

import za.co.capitec.transactiondispute.disputes.domain.model.DisputeStatus;
import za.co.capitec.transactiondispute.disputes.domain.model.TransactionStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record DisputeTransactionViewRow(
        UUID disputeId,
        UUID transactionId,
        UUID userId,
        DisputeStatus disputeStatus,
        String reasonCode,
        String disputeNote,
        Instant createdAt,

        Instant postedAt,
        BigDecimal amount,
        String currency,
        String merchant,
        TransactionStatus transactionStatus,
        String rrn
) {}
