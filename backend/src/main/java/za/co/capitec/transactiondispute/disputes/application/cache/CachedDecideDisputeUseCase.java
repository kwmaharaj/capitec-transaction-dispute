package za.co.capitec.transactiondispute.disputes.application.cache;

import reactor.core.publisher.Mono;
import za.co.capitec.transactiondispute.disputes.application.port.in.DecideDisputeUseCase;
import za.co.capitec.transactiondispute.disputes.domain.model.Dispute;
import za.co.capitec.transactiondispute.disputes.domain.model.DisputeStatus;
import za.co.capitec.transactiondispute.shared.application.port.out.TransactionCacheInvalidationPort;

import java.util.Objects;
import java.util.UUID;

/**
 * Mutations are DB source of truth. Cache invalidation is best-effort + fail-open.
 */
public class CachedDecideDisputeUseCase implements DecideDisputeUseCase {

    private final DecideDisputeUseCase delegate;
    private final CachedFindUserDisputesUseCase userDisputesCache;
    private final CachedFindDisputesUseCase supportDisputesCache;
    private final CachedDisputeViewQueryUseCase viewCache;
    private final CachedDisputeHistoryUseCase historyCache;
    private final TransactionCacheInvalidationPort txCache;

    public CachedDecideDisputeUseCase(DecideDisputeUseCase delegate,
                                     CachedFindUserDisputesUseCase userDisputesCache,
                                     CachedFindDisputesUseCase supportDisputesCache,
                                     CachedDisputeViewQueryUseCase viewCache,
                                     CachedDisputeHistoryUseCase historyCache,
                                     TransactionCacheInvalidationPort txCache) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.userDisputesCache = Objects.requireNonNull(userDisputesCache, "userDisputesCache");
        this.supportDisputesCache = Objects.requireNonNull(supportDisputesCache, "supportDisputesCache");
        this.viewCache = Objects.requireNonNull(viewCache, "viewCache");
        this.historyCache = Objects.requireNonNull(historyCache, "historyCache");
        this.txCache = Objects.requireNonNull(txCache, "txCache");
    }

    @Override
    public Mono<Dispute> decide(UUID disputeId, UUID supportUserId, DisputeStatus disputeStatus, String supportNote) {
        return delegate.decide(disputeId, supportUserId, disputeStatus, supportNote)
                .flatMap(saved -> invalidateAfterDecision(saved).thenReturn(saved));
    }

    private Mono<Void> invalidateAfterDecision(Dispute saved) {
        return userDisputesCache.invalidate(saved.userId())
                .then(viewCache.invalidateUser(saved.userId()))
                .then(viewCache.invalidateSupport())
                .then(supportDisputesCache.invalidate())
                .then(historyCache.invalidateForUser(saved.disputeId(), saved.userId()))
                .then(historyCache.invalidateForSupport(saved.disputeId()))
                .then(txCache.invalidateAfterTransactionChange(saved.transactionId(), saved.userId()))
                .onErrorResume(e -> Mono.empty());
    }
}
