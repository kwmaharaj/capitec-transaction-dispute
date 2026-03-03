package za.co.capitec.transactiondispute.testsupport.cache;

import reactor.core.publisher.Mono;
import za.co.capitec.transactiondispute.shared.infrastructure.cache.ReactiveCacheStore;

import java.time.Duration;

/**
 * Cache store that always fails, to verify fail-open behavior.
 */
public class FailingReactiveCacheStore implements ReactiveCacheStore {

    private final RuntimeException failure;

    public FailingReactiveCacheStore(RuntimeException failure) {
        this.failure = failure;
    }

    @Override
    public Mono<String> get(String key) {
        return Mono.error(failure);
    }

    @Override
    public Mono<Boolean> set(String key, String value, Duration ttl) {
        return Mono.error(failure);
    }

    @Override
    public Mono<Boolean> delete(String key) {
        return Mono.error(failure);
    }

    @Override
    public Mono<Long> increment(String key) {
        return Mono.error(failure);
    }
}
