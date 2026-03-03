package za.co.capitec.transactiondispute.transactions.application;

import java.util.UUID;

import org.springframework.stereotype.Component;

import reactor.core.publisher.Mono;
import za.co.capitec.transactiondispute.shared.application.port.out.TransactionCacheInvalidationPort;

/**
 * Adapter inside transactions module implementing shared port.
 * Keeps disputes module decoupled from transactions module.
 */
@Component
public class TransactionCacheInvalidationAdapter implements TransactionCacheInvalidationPort {

    private final CachedTransactionQueryUseCase txCache;

    public TransactionCacheInvalidationAdapter(CachedTransactionQueryUseCase txCache) {
        this.txCache = txCache;
    }

    @Override
    public Mono<Void> invalidateAfterTransactionChange(UUID userId, UUID transactionId) {
        return txCache.invalidateAfterTransactionChange(userId, transactionId);
    }
}