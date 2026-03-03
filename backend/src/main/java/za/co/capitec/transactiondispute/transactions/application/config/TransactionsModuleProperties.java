package za.co.capitec.transactiondispute.transactions.application.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import za.co.capitec.transactiondispute.transactions.domain.model.TransactionStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@ConfigurationProperties(prefix = "modules.transactions")
public record TransactionsModuleProperties(
        List<Transaction> transactionsToSeed
) {

    public record Transaction(UUID transactionId,
                              UUID userId,
                              Instant postedAt,
                              BigDecimal amount,
                              String currency,
                              String merchant,
                              TransactionStatus transactionStatus,
                              String bin,
                              String last4digits,
                              String panHash,//not storing full pan, ie encrypted at rest...
                              String rrn) {}
}