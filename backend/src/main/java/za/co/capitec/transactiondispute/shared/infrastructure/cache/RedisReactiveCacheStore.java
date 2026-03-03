package za.co.capitec.transactiondispute.shared.infrastructure.cache;

import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Objects;

/**
 * Redis-backed implementation using String keys and String (JSON) values.
 */
public class RedisReactiveCacheStore implements ReactiveCacheStore {

    private final ReactiveStringRedisTemplate redis;

    public RedisReactiveCacheStore(ReactiveStringRedisTemplate redis) {
        this.redis = Objects.requireNonNull(redis, "redis");
    }

    @Override
    public Mono<String> get(String key) {
        return redis.opsForValue().get(key);
    }

    @Override
    public Mono<Boolean> set(String key, String value, Duration ttl) {
        return redis.opsForValue().set(key, value, ttl);
    }

    @Override
    public Mono<Boolean> delete(String key) {
        return redis.delete(key).map(count -> count != null && count > 0);
    }

    @Override
    public Mono<Long> increment(String key) {
        return redis.opsForValue().increment(key);
    }
}
