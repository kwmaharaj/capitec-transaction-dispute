package za.co.capitec.transactiondispute.transactions.application.port.in;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import za.co.capitec.transactiondispute.transactions.application.model.TransactionSearchCriteria;
import za.co.capitec.transactiondispute.transactions.application.model.TransactionView;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface TransactionQueryUseCase {

    Flux<TransactionView> findForUser(UUID userId);

    Flux<TransactionView> findAllForSupport();

    Mono<TransactionView> findById(UUID transactionId);

    Mono<Page<TransactionView>> searchForUser(UUID userId, TransactionSearchCriteria criteria, Pageable pageable);

    Mono<Page<TransactionView>> searchAllForSupport(TransactionSearchCriteria criteria, Pageable pageable);
}