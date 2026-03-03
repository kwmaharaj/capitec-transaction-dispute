package za.co.capitec.transactiondispute.transactions.application;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import za.co.capitec.transactiondispute.transactions.application.port.in.TransactionIngestionUseCase;
import za.co.capitec.transactiondispute.transactions.application.port.out.TransactionRepositoryPort;
import za.co.capitec.transactiondispute.transactions.domain.model.Transaction;

public class TransactionIngestionService implements TransactionIngestionUseCase {

    private final TransactionRepositoryPort repo;

    public TransactionIngestionService(TransactionRepositoryPort repo) {
        this.repo = repo;
    }

    @Override
    public Mono<Void> ingest(Flux<Transaction> transactions) {
        return repo.upsertAll(transactions);
    }
}
