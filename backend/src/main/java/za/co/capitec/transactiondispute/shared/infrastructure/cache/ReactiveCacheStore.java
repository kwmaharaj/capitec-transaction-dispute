package za.co.capitec.transactiondispute.shared.infrastructure.cache;

import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Minimal reactive KV operations needed for cache-aside + namespace versioning.
 */
public interface ReactiveCacheStore {

    Mono<String> get(String key);

    Mono<Boolean> set(String key, String value, Duration ttl);

    Mono<Boolean> delete(String key);

    /**
     * Atomically increments a key and returns the new value.
     */
    Mono<Long> increment(String key);
}
