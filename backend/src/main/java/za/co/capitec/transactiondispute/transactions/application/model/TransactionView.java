package za.co.capitec.transactiondispute.transactions.application.model;

import za.co.capitec.transactiondispute.transactions.domain.model.TransactionStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransactionView(
        UUID transactionId,
        UUID userId,
        Instant postedAt,
        BigDecimal amount,
        String currency,
        String merchant,
        TransactionStatus transactionStatus,
        String bin,
        String last4digits,
        String panHash,
        String rrn
) {
}