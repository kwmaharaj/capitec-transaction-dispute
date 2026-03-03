package za.co.capitec.transactiondispute.transactions.application.model;

import za.co.capitec.transactiondispute.transactions.domain.model.TransactionStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record TransactionSearchCriteria(
        BigDecimal minAmount,
        BigDecimal maxAmount,
        String rrn,
        String merchant,
        TransactionStatus transactionStatus,
        Instant fromDate,
        Instant toDate
) {
    public static TransactionSearchCriteria empty() {
        return new TransactionSearchCriteria(null, null, null, null, null, null, null);
    }
}
