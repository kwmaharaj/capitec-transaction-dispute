package za.co.capitec.transactiondispute.transactions.application;

import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import za.co.capitec.transactiondispute.shared.infrastructure.cache.CacheKeyFactory;
import za.co.capitec.transactiondispute.shared.infrastructure.cache.CacheNames;
import za.co.capitec.transactiondispute.shared.infrastructure.cache.ReactiveCacheAside;
import za.co.capitec.transactiondispute.transactions.application.model.TransactionSearchCriteria;
import za.co.capitec.transactiondispute.transactions.application.model.TransactionView;
import za.co.capitec.transactiondispute.transactions.application.port.in.TransactionQueryUseCase;

import java.util.Objects;
import java.util.UUID;

/**
 * Cache-aside wrapper for transaction GET endpoints.
 */
public class CachedTransactionQueryUseCase implements TransactionQueryUseCase {

    private final TransactionQueryUseCase delegate;
    private final ReactiveCacheAside cache;
    private final CacheKeyFactory keys;

    public CachedTransactionQueryUseCase(TransactionQueryUseCase delegate,
                                         ReactiveCacheAside cache,
                                         CacheKeyFactory keys) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.cache = Objects.requireNonNull(cache, "cache");
        this.keys = Objects.requireNonNull(keys, "keys");
    }

    @Override
    public Flux<TransactionView> findForUser(UUID userId) {
        // Not currently exposed via controller; keep uncached for now.
        return delegate.findForUser(userId);
    }

    @Override
    public Flux<TransactionView> findAllForSupport() {
        // Not currently exposed via controller; keep uncached for now.
        return delegate.findAllForSupport();
    }

    @Override
    public Mono<TransactionView> findById(UUID transactionId) {
        String key = keys.simple(CacheNames.TX_BY_ID, transactionId.toString());
        return cache.getOrLoad(
                CacheNames.TX_BY_ID,
                Mono.just(key),
                new TypeReference<>() {},
                () -> delegate.findById(transactionId)
        );
    }

    @Override
    public Mono<Page<TransactionView>> searchForUser(UUID userId, TransactionSearchCriteria criteria, Pageable pageable) {
        String hash = keys.hash(new KeyParts(criteria, pageable));
        return cache.getOrLoad(
                CacheNames.TX_SEARCH_USER,
                keys.versioned(namespaceTxSearchUser(userId), hash),
                new TypeReference<>() {},
                () -> delegate.searchForUser(userId, criteria, pageable)
        );
    }

    @Override
    public Mono<Page<TransactionView>> searchAllForSupport(TransactionSearchCriteria criteria, Pageable pageable) {
        String hash = keys.hash(new KeyParts(criteria, pageable));
        return cache.getOrLoad(
                CacheNames.TX_SEARCH_SUPPORT,
                keys.versioned(CacheNames.TX_SEARCH_SUPPORT, hash),
                new TypeReference<>() {},
                () -> delegate.searchAllForSupport(criteria, pageable)
        );
    }

    public Mono<Void> invalidateAfterTransactionChange(UUID transactionId, UUID userId) {
        // delete the by-id key and bump list/search namespaces
        String byIdKey = keys.simple(CacheNames.TX_BY_ID, transactionId.toString());
        return cache.deleteBestEffort(byIdKey)
                .then(keys.bumpNamespaceVersion(namespaceTxSearchUser(userId)).then())
                .then(keys.bumpNamespaceVersion(CacheNames.TX_SEARCH_SUPPORT).then())
                .onErrorResume(e -> Mono.empty()); // extra safety
    }


    public Mono<Void> invalidateSearchesForUser(UUID userId) {
        return keys.bumpNamespaceVersion(namespaceTxSearchUser(userId)).then().onErrorResume(e -> Mono.empty());
    }

    public Mono<Void> invalidateSupportSearches() {
        return keys.bumpNamespaceVersion(CacheNames.TX_SEARCH_SUPPORT).then().onErrorResume(e -> Mono.empty());
    }

    private static String namespaceTxSearchUser(UUID userId) {
        return CacheNames.TX_SEARCH_USER + ":" + userId;
    }

    private record KeyParts(TransactionSearchCriteria criteria, Pageable pageable) {}
}
