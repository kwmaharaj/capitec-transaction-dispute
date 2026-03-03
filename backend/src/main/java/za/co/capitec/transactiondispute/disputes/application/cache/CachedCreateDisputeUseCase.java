package za.co.capitec.transactiondispute.disputes.application.cache;

import reactor.core.publisher.Mono;
import za.co.capitec.transactiondispute.disputes.application.model.CreateDisputeCommand;
import za.co.capitec.transactiondispute.disputes.application.model.CreateDisputeResult;
import za.co.capitec.transactiondispute.disputes.application.port.in.CreateDisputeUseCase;
import za.co.capitec.transactiondispute.shared.application.port.out.TransactionCacheInvalidationPort;

import java.util.Objects;

/**
 * Mutations are DB source of truth. Cache invalidation is best-effort + fail-open.
 */
public class CachedCreateDisputeUseCase implements CreateDisputeUseCase {

    private final CreateDisputeUseCase delegate;
    private final CachedFindUserDisputesUseCase userDisputesCache;
    private final CachedFindDisputesUseCase supportDisputesCache;
    private final CachedDisputeViewQueryUseCase viewCache;
    private final CachedDisputeHistoryUseCase historyCache;
    private final TransactionCacheInvalidationPort txCache;

    public CachedCreateDisputeUseCase(CreateDisputeUseCase delegate,
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
    public Mono<CreateDisputeResult> create(CreateDisputeCommand command) {
        return delegate.create(command)
                .flatMap(result ->
                        invalidateAfterCreate(command, result).thenReturn(result)
                );
    }

    private Mono<Void> invalidateAfterCreate(CreateDisputeCommand command, CreateDisputeResult result) {
        // Best-effort invalidation; never fail the request.
        return userDisputesCache.invalidate(command.userId())
                .then(viewCache.invalidateUser(command.userId()))
                .then(viewCache.invalidateSupport())
                .then(supportDisputesCache.invalidate())
                // new dispute history now exists
                .then(historyCache.invalidateForUser(result.disputeId(), command.userId()))
                .then(historyCache.invalidateForSupport(result.disputeId()))
                // transaction might be involved in views/filters
                .then(txCache.invalidateAfterTransactionChange(result.transactionId(), command.userId()))
                .onErrorResume(e -> Mono.empty());
    }
}
