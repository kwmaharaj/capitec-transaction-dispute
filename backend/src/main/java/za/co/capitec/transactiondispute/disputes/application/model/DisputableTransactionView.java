package za.co.capitec.transactiondispute.disputes.application.model;

import za.co.capitec.transactiondispute.disputes.domain.model.TransactionStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record DisputableTransactionView(
        UUID transactionId,
        UUID accountId,
        Instant postedAt,
        BigDecimal amount,
        String currency,
        String merchant,
        TransactionStatus transactionStatus
) {}
