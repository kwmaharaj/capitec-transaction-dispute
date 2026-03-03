package za.co.capitec.transactiondispute.security;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import za.co.capitec.transactiondispute.security.application.TokenRevocationService;

import java.time.Duration;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TokenRevocationServiceTest {

    @Test
    void revoke_with_blank_jti_or_non_positive_ttl_is_noop() {
        var redis = mock(ReactiveStringRedisTemplate.class);
        var svc = new TokenRevocationService(redis);

        StepVerifier.create(svc.revoke(" ", Duration.ofSeconds(10))).verifyComplete();
        StepVerifier.create(svc.revoke("jti", Duration.ZERO)).verifyComplete();
        StepVerifier.create(svc.revoke("jti", Duration.ofSeconds(-1))).verifyComplete();

        verifyNoInteractions(redis);
    }

    @Test
    void revoke_with_valid_inputs_sets_redis_key_with_ttl() {
        ReactiveStringRedisTemplate redis = mock(ReactiveStringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ReactiveValueOperations<String, String> ops = mock(ReactiveValueOperations.class);

        when(redis.opsForValue()).thenReturn(ops);
        when(ops.set(anyString(), anyString(), any(Duration.class))).thenReturn(Mono.just(true));

        var svc = new TokenRevocationService(redis);

        StepVerifier.create(svc.revoke("abc", Duration.ofMinutes(1))).verifyComplete();

        verify(ops, times(1)).set(startsWith("revoked:jwt:"), eq("1"), eq(Duration.ofMinutes(1)));
    }

    @Test
    void isRevoked_with_blank_jti_returns_false_without_touching_redis() {
        var redis = mock(ReactiveStringRedisTemplate.class);
        var svc = new TokenRevocationService(redis);

        StepVerifier.create(svc.isRevoked(" "))
                .expectNext(false)
                .verifyComplete();

        verifyNoInteractions(redis);
    }
}
