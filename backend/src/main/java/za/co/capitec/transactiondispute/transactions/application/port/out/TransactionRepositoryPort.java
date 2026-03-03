package za.co.capitec.transactiondispute.transactions.application.port.out;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import za.co.capitec.transactiondispute.transactions.application.model.TransactionSearchCriteria;
import za.co.capitec.transactiondispute.transactions.domain.model.Transaction;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface TransactionRepositoryPort {

    Flux<Transaction> findByUserId(UUID userId);

    Flux<Transaction> findAll();

    Mono<Transaction> findById(UUID transactionId);

    Mono<Void> upsertAll(Flux<Transaction> transactions);

    Mono<Page<Transaction>> searchForUser(UUID userId, TransactionSearchCriteria criteria, Pageable pageable);

    Mono<Page<Transaction>> searchAll(TransactionSearchCriteria criteria, Pageable pageable);
}