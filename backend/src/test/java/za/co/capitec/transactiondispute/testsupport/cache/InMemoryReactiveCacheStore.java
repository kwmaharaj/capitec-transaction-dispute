package za.co.capitec.transactiondispute.testsupport.cache;

import reactor.core.publisher.Mono;
import za.co.capitec.transactiondispute.shared.infrastructure.cache.ReactiveCacheStore;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * In-memory ReactiveCacheStore for unit tests.
 */
public class InMemoryReactiveCacheStore implements ReactiveCacheStore {

    private final Map<String, String> values = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> counters = new ConcurrentHashMap<>();

    @Override
    public Mono<String> get(String key) {
        return Mono.justOrEmpty(values.get(key));
    }

    @Override
    public Mono<Boolean> set(String key, String value, Duration ttl) {
        values.put(key, value);
        return Mono.just(true);
    }

    @Override
    public Mono<Boolean> delete(String key) {
        return Mono.just(values.remove(key) != null);
    }

    @Override
    public Mono<Long> increment(String key) {
        AtomicLong c = counters.computeIfAbsent(key, k -> new AtomicLong(0));
        long v = c.incrementAndGet();
        values.put(key, Long.toString(v)); // keep store.get() consistent
        return Mono.just(v);
    }
}
