package za.co.capitec.transactiondispute.shared.application.port.out;

import java.util.UUID;
import reactor.core.publisher.Mono;

/**
 * Cross-module cache invalidation port.
 * Implemented by the transactions module, used by other modules (e.g. disputes) without needing to create module dependencies.
 */
public interface TransactionCacheInvalidationPort {

    /**
     * Best-effort invalidation for transaction-related cache entries.
     *
     * @param userId the owning user (may be required for scoping)
     * @param transactionId the transaction id
     */
    Mono<Void> invalidateAfterTransactionChange(UUID userId, UUID transactionId);
}