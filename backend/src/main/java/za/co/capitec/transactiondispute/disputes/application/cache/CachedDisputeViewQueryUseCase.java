package za.co.capitec.transactiondispute.disputes.application.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Mono;
import za.co.capitec.transactiondispute.disputes.application.model.DisputeSearchCriteria;
import za.co.capitec.transactiondispute.disputes.application.model.DisputeTransactionViewRow;
import za.co.capitec.transactiondispute.disputes.application.port.in.DisputeViewQueryUseCase;
import za.co.capitec.transactiondispute.shared.infrastructure.cache.CacheKeyFactory;
import za.co.capitec.transactiondispute.shared.infrastructure.cache.CacheNames;
import za.co.capitec.transactiondispute.shared.infrastructure.cache.ReactiveCacheAside;

import java.util.Objects;
import java.util.UUID;

public class CachedDisputeViewQueryUseCase implements DisputeViewQueryUseCase {

    private final DisputeViewQueryUseCase delegate;
    private final ReactiveCacheAside cache;
    private final CacheKeyFactory keys;

    public CachedDisputeViewQueryUseCase(DisputeViewQueryUseCase delegate, ReactiveCacheAside cache, CacheKeyFactory keys) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.cache = Objects.requireNonNull(cache, "cache");
        this.keys = Objects.requireNonNull(keys, "keys");
    }

    @Override
    public Mono<Page<DisputeTransactionViewRow>> searchForUser(UUID userId, DisputeSearchCriteria criteria, Pageable pageable) {
        String hash = keys.hash(new KeyParts(criteria, pageable));
        return cache.getOrLoad(
                CacheNames.DISPUTE_VIEW_USER,
                keys.versioned(namespaceUser(userId), hash),
                new TypeReference<>() {},
                () -> delegate.searchForUser(userId, criteria, pageable)
        );
    }

    @Override
    public Mono<Page<DisputeTransactionViewRow>> searchForSupport(DisputeSearchCriteria criteria, Pageable pageable) {
        String hash = keys.hash(new KeyParts(criteria, pageable));
        return cache.getOrLoad(
                CacheNames.DISPUTE_VIEW_SUPPORT,
                keys.versioned(CacheNames.DISPUTE_VIEW_SUPPORT, hash),
                new TypeReference<>() {},
                () -> delegate.searchForSupport(criteria, pageable)
        );
    }

    public Mono<Void> invalidateUser(UUID userId) {
        return keys.bumpNamespaceVersion(namespaceUser(userId)).then();
    }

    public Mono<Void> invalidateSupport() {
        return keys.bumpNamespaceVersion(CacheNames.DISPUTE_VIEW_SUPPORT).then();
    }

    private static String namespaceUser(UUID userId) {
        return CacheNames.DISPUTE_VIEW_USER + ":" + userId;
    }

    private record KeyParts(DisputeSearchCriteria criteria, Pageable pageable) {}
}
