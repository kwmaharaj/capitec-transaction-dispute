package za.co.capitec.transactiondispute.security.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Stores revoked access-token JTIs in Redis with a TTL equal to the remaining token lifetime.
 *
 * IMPORTANT: Redis is treated as an availability-dependent component.
 * If Redis is down, we FAIL OPEN (treat token as not revoked) to keep the system operational.
 */
public class TokenRevocationService {

    private static final Logger log = LoggerFactory.getLogger(TokenRevocationService.class);

    private static final String KEY_PREFIX = "revoked:jwt:";

    private final ReactiveStringRedisTemplate redis;

    public TokenRevocationService(ReactiveStringRedisTemplate redis) {
        this.redis = redis;
    }

    public Mono<Void> revoke(String jti, Duration ttl) {
        if (jti == null || jti.isBlank() || ttl == null || ttl.isNegative() || ttl.isZero()) {
            return Mono.empty();
        }
        return redis.opsForValue()
                .set(KEY_PREFIX + jti, "1", ttl)
                .then()
                .onErrorResume(ex -> {
                    log.warn("Redis unavailable; skipping token revocation write (fail-open). jti={}", jti, ex);
                    return Mono.empty();
                });
    }

    public Mono<Boolean> isRevoked(String jti) {
        if (jti == null || jti.isBlank()) {
            return Mono.just(false);
        }
        return redis.hasKey(KEY_PREFIX + jti)
                .defaultIfEmpty(false)
                .onErrorResume(ex -> {
                    log.warn("Redis unavailable; treating token as NOT revoked (fail-open). jti={}", jti, ex);
                    return Mono.just(false);
                });
    }
}