package za.co.capitec.transactiondispute.transactions.application;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import za.co.capitec.transactiondispute.transactions.application.model.TransactionSearchCriteria;
import za.co.capitec.transactiondispute.transactions.application.model.TransactionView;
import za.co.capitec.transactiondispute.transactions.application.port.in.TransactionQueryUseCase;
import za.co.capitec.transactiondispute.transactions.application.port.out.TransactionRepositoryPort;
import za.co.capitec.transactiondispute.transactions.domain.model.Transaction;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public class TransactionQueryService implements TransactionQueryUseCase {

    private final TransactionRepositoryPort repo;

    public TransactionQueryService(TransactionRepositoryPort repo) {
        this.repo = repo;
    }

    @Override
    public Flux<TransactionView> findForUser(UUID userId) {
        return repo.findByUserId(userId).map(TransactionQueryService::toView);
    }

    @Override
    public Flux<TransactionView> findAllForSupport() {
        return repo.findAll().map(TransactionQueryService::toView);
    }

    @Override
    public Mono<TransactionView> findById(UUID transactionId) {
        return repo.findById(transactionId).map(TransactionQueryService::toView);
    }

    @Override
    public Mono<Page<TransactionView>> searchForUser(UUID userId, TransactionSearchCriteria criteria, Pageable pageable) {
        return repo.searchForUser(userId, criteria, pageable).map(page -> page.map(TransactionQueryService::toView));
    }

    @Override
    public Mono<Page<TransactionView>> searchAllForSupport(TransactionSearchCriteria criteria, Pageable pageable) {
        return repo.searchAll(criteria, pageable)
                .map(page -> page.map(TransactionQueryService::toView));
    }

    private static TransactionView toView(Transaction t) {
        return new TransactionView(
                t.transactionId(),
                t.userId(),
                t.postedAt(),
                t.amount(),
                t.currency(),
                t.merchant(),
                t.transactionStatus(),
                t.bin(),
                t.last4digits(),
                t.panHash(),
                t.rrn()
        );
    }
}