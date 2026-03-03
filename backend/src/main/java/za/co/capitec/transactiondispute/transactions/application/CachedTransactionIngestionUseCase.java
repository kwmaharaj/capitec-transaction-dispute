package za.co.capitec.transactiondispute.transactions.application;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import za.co.capitec.transactiondispute.transactions.application.port.in.TransactionIngestionUseCase;
import za.co.capitec.transactiondispute.transactions.domain.model.Transaction;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Mutations are DB source of truth. Cache invalidation is best-effort + fail-open.
 */
public class CachedTransactionIngestionUseCase implements TransactionIngestionUseCase {

    private final TransactionIngestionUseCase delegate;
    private final CachedTransactionQueryUseCase txCache;

    public CachedTransactionIngestionUseCase(TransactionIngestionUseCase delegate, CachedTransactionQueryUseCase txCache) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.txCache = Objects.requireNonNull(txCache, "txCache");
    }

    @Override
    public Mono<Void> ingest(Flux<Transaction> transactions) {
        return transactions
                .collectList()
                .flatMap(list ->
                        delegate.ingest(Flux.fromIterable(list))
                                .then(invalidateAfterIngest(list))
                );
    }

    private Mono<Void> invalidateAfterIngest(java.util.List<Transaction> list) {
        // Best-effort: invalidate by-id + bump per-user list/search namespace.
        Set<UUID> users = list.stream().map(Transaction::userId).collect(Collectors.toSet());

        Mono<Void> byIdDeletes = Flux.fromIterable(list)
                .flatMap(tx -> txCache.invalidateAfterTransactionChange(tx.transactionId(), tx.userId()))
                .then();

        Mono<Void> bumpUsers = Flux.fromIterable(users)
                .flatMap(txCache::invalidateSearchesForUser)
                .then();

        Mono<Void> bumpSupport = txCache.invalidateSupportSearches();

        return byIdDeletes.then(bumpUsers).then(bumpSupport).onErrorResume(e -> Mono.empty());
    }
}
