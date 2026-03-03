package za.co.capitec.transactiondispute.transactions.interfaces.http;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import za.co.capitec.transactiondispute.transactions.application.port.in.TransactionIngestionUseCase;
import za.co.capitec.transactiondispute.transactions.domain.model.Transaction;
import za.co.capitec.transactiondispute.transactions.domain.model.TransactionStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * INTERNAL!!: Seed demo/test data via HTTP.
 *
 * Protected by ROLE_SUPPORT via SecurityConfig.
 */
@RestController
@RequestMapping("/internal/ingest")
public class TransactionIngestionController {

    private final TransactionIngestionUseCase ingestion;

    public TransactionIngestionController(TransactionIngestionUseCase ingestion) {
        this.ingestion = ingestion;
    }

    @PostMapping("/transactions")
    public Mono<Void> ingestTransactions(@RequestBody Flux<IngestTransactionRequest> body) {
        return ingestion.ingest(body.map(IngestTransactionRequest::toDomain));
    }

    public record IngestTransactionRequest(
            UUID transactionId,
            UUID userId,
            Instant postedAt,
            BigDecimal amount,
            String currency,
            String merchant,
            TransactionStatus status,
            String bin,
            String last4digits,
            String panHash,
            String rrn
    ) {
        Transaction toDomain() {
            return new Transaction(
                    transactionId,
                    userId,
                    postedAt,
                    amount,
                    currency,
                    merchant,
                    status,
                    bin,
                    last4digits,
                    panHash,
                    rrn
            );
        }
    }
}
