package za.co.capitec.transactiondispute.transactions.application.port.in;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import za.co.capitec.transactiondispute.transactions.domain.model.Transaction;

/**
 * Internal ingestion API for seeding demo/test data via HTTP.
 *
 * Note: This is intentionally separated from normal business use-cases.
 */
public interface TransactionIngestionUseCase {
    Mono<Void> ingest(Flux<Transaction> transactions);
}
