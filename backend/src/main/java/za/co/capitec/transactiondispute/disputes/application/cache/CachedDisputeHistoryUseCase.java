package za.co.capitec.transactiondispute.disputes.application.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import za.co.capitec.transactiondispute.disputes.application.model.DisputeHistoryItem;
import za.co.capitec.transactiondispute.disputes.application.port.in.DisputeHistoryUseCase;
import za.co.capitec.transactiondispute.shared.infrastructure.cache.CacheKeyFactory;
import za.co.capitec.transactiondispute.shared.infrastructure.cache.CacheNames;
import za.co.capitec.transactiondispute.shared.infrastructure.cache.ReactiveCacheAside;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class CachedDisputeHistoryUseCase implements DisputeHistoryUseCase {

    private final DisputeHistoryUseCase delegate;
    private final ReactiveCacheAside cache;
    private final CacheKeyFactory keys;

    public CachedDisputeHistoryUseCase(DisputeHistoryUseCase delegate, ReactiveCacheAside cache, CacheKeyFactory keys) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.cache = Objects.requireNonNull(cache, "cache");
        this.keys = Objects.requireNonNull(keys, "keys");
    }

    @Override
    public Flux<DisputeHistoryItem> findForUser(UUID disputeId, UUID userId) {
        String key = keys.simple(CacheNames.DISPUTE_HISTORY_USER, userId.toString(), disputeId.toString());

        Mono<List<DisputeHistoryItem>> cached = cache.getOrLoad(
                CacheNames.DISPUTE_HISTORY_USER,
                Mono.just(key),
                new TypeReference<>() {},
                () -> delegate.findForUser(disputeId, userId).collectList()
        );

        return cached.flatMapMany(Flux::fromIterable);
    }

    @Override
    public Flux<DisputeHistoryItem> findForSupport(UUID disputeId) {
        String key = keys.simple(CacheNames.DISPUTE_HISTORY_SUPPORT, disputeId.toString());

        Mono<List<DisputeHistoryItem>> cached = cache.getOrLoad(
                CacheNames.DISPUTE_HISTORY_SUPPORT,
                Mono.just(key),
                new TypeReference<>() {},
                () -> delegate.findForSupport(disputeId).collectList()
        );

        return cached.flatMapMany(Flux::fromIterable);
    }

    public Mono<Void> invalidateForUser(UUID disputeId, UUID userId) {
        String key = keys.simple(CacheNames.DISPUTE_HISTORY_USER, userId.toString(), disputeId.toString());
        return cache.deleteBestEffort(key);
    }

    public Mono<Void> invalidateForSupport(UUID disputeId) {
        String key = keys.simple(CacheNames.DISPUTE_HISTORY_SUPPORT, disputeId.toString());
        return cache.deleteBestEffort(key);
    }
}
