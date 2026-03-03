package za.co.capitec.transactiondispute.shared.infrastructure.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Cache-aside helper:
 * - cache read failures are fail-open (treated as miss)
 * - cache write failures are fail-open (ignore)
 */
public class ReactiveCacheAside {

    private final ReactiveCacheStore store;
    private final JsonCacheCodec codec;
    private final CacheProperties props;

    public ReactiveCacheAside(ReactiveCacheStore store, JsonCacheCodec codec, CacheProperties props) {
        this.store = Objects.requireNonNull(store, "store");
        this.codec = Objects.requireNonNull(codec, "codec");
        this.props = Objects.requireNonNull(props, "props");
    }

    public <T> Mono<T> getOrLoad(String cacheName, Mono<String> keyMono, TypeReference<T> type, Supplier<Mono<T>> loader) {
        Duration ttl = props.ttlFor(cacheName);

        return keyMono.flatMap(key ->
                store.get(key)
                        .flatMap(json -> Mono.fromCallable(() -> codec.read(json, type)))
                        .onErrorResume(e -> Mono.empty()) // fail-open -> miss
                        .switchIfEmpty(
                                loader.get()
                                        .flatMap(val ->
                                                Mono.fromCallable(() -> codec.write(val))
                                                        .flatMap(json ->
                                                                store.set(key, json, ttl)
                                                                        .onErrorResume(e -> Mono.just(false)) // fail-open cache write
                                                                        .thenReturn(val)
                                                        )
                                                        .onErrorResume(e -> Mono.just(val)) // fail-open serialization
                                        )
                        )
        );
    }

    public Mono<Void> deleteBestEffort(String key) {
        return store.delete(key)
                .onErrorResume(e -> Mono.just(false)) // fail-open
                .then();
    }
}
