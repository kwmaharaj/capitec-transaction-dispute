package za.co.capitec.transactiondispute.transactions.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record Transaction(
        UUID transactionId,
        UUID userId,
        Instant postedAt,
        BigDecimal amount,
        String currency,
        String merchant,
        TransactionStatus transactionStatus,
        String bin,
        String last4digits,
        String panHash,//not storing full pan, ie encrypted at rest...
        String rrn
) {
}