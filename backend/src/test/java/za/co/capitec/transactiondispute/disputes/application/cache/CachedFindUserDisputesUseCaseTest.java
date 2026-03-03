package za.co.capitec.transactiondispute.disputes.application.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import za.co.capitec.transactiondispute.disputes.application.port.in.FindUserDisputesUseCase;
import za.co.capitec.transactiondispute.disputes.domain.model.Dispute;
import za.co.capitec.transactiondispute.disputes.domain.model.ReasonCode;
import za.co.capitec.transactiondispute.shared.infrastructure.cache.*;
import za.co.capitec.transactiondispute.testsupport.cache.FailingReactiveCacheStore;
import za.co.capitec.transactiondispute.testsupport.cache.InMemoryReactiveCacheStore;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;

class CachedFindUserDisputesUseCaseTest {

    @Test
    void cacheMiss_loadsFromDelegate_andWritesToCache() {
        FindUserDisputesUseCase delegate = mock(FindUserDisputesUseCase.class);

        ObjectMapper om = new ObjectMapper().findAndRegisterModules();
        var store = new InMemoryReactiveCacheStore();
        var props = new CacheProperties();
        var codec = new JsonCacheCodec(om);
        var cache = new ReactiveCacheAside(store, codec, props);
        var keys = new CacheKeyFactory(store, om);

        UUID disputeId = UUID.randomUUID();
        UUID txId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant createdAt = Instant.now();

        Dispute d1 = Dispute.openDispute(
                disputeId,
                txId,
                userId,
                ReasonCode.FRAUD_SUSPECTED,
                "n",
                createdAt
        );

        when(delegate.findByUserId(userId)).thenReturn(Flux.just(d1));

        var sut = new CachedFindUserDisputesUseCase(delegate, cache, keys);

        StepVerifier.create(sut.findByUserId(userId))
                .expectNextMatches(d ->
                        d.disputeId().equals(disputeId)
                                && d.transactionId().equals(txId)
                                && d.userId().equals(userId)
                                && d.reasonCode() == ReasonCode.FRAUD_SUSPECTED
                                && d.note().equals("n")
                                && d.createdAt().toString().equals(createdAt.toString())
                )
                .verifyComplete();

        verify(delegate, times(1)).findByUserId(userId);

        StepVerifier.create(store.get(keys.simple(CacheNames.DISPUTES_BY_USER, userId.toString())))
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    void cacheHit_doesNotCallDelegate() {
        FindUserDisputesUseCase delegate = mock(FindUserDisputesUseCase.class);

        ObjectMapper om = new ObjectMapper().findAndRegisterModules();
        var store = new InMemoryReactiveCacheStore();
        var props = new CacheProperties();
        var codec = new JsonCacheCodec(om);
        var cache = new ReactiveCacheAside(store, codec, props);
        var keys = new CacheKeyFactory(store, om);

        UUID disputeId = UUID.randomUUID();
        UUID txId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant createdAt = Instant.now();

        Dispute d1 = Dispute.openDispute(
                disputeId,
                txId,
                userId,
                ReasonCode.FRAUD_SUSPECTED,
                "n",
                createdAt
        );

        var sut = new CachedFindUserDisputesUseCase(delegate, cache, keys);

        // Seed cache with the DTO type the SUT actually caches
        String key = keys.simple(CacheNames.DISPUTES_BY_USER, userId.toString());
        var dto = CachedFindUserDisputesUseCase.DisputeCacheDto.fromDomain(d1);

        Mono<Void> seed = store
                .set(key, codec.write(List.of(dto)), props.ttlFor(CacheNames.DISPUTES_BY_USER))
                .then();

        // delegate must never be called!!
        when(delegate.findByUserId(userId))
                .thenReturn(Flux.error(new AssertionError("Delegate should not be called")));

        StepVerifier.create(seed.thenMany(sut.findByUserId(userId)))
                .expectNextMatches(d ->
                        d.disputeId().equals(disputeId)
                                && d.transactionId().equals(txId)
                                && d.userId().equals(userId)
                                && d.reasonCode() == ReasonCode.FRAUD_SUSPECTED
                                && d.note().equals("n")
                                && d.createdAt().toString().equals(createdAt.toString())
                )
                .verifyComplete();

        verify(delegate, atMostOnce()).findByUserId(any());
        //ensure actually had cache content
        StepVerifier.create(store.get(key))
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    void failOpen_whenRedisFails_stillReturnsDelegateValue() {
        FindUserDisputesUseCase delegate = mock(FindUserDisputesUseCase.class);

        ObjectMapper om = new ObjectMapper().findAndRegisterModules();
        var store = new FailingReactiveCacheStore(new RuntimeException("redis down"));
        var props = new CacheProperties();
        var cache = new ReactiveCacheAside(store, new JsonCacheCodec(om), props);
        var keys = new CacheKeyFactory(store, om);

        UUID disputeId = UUID.randomUUID();
        UUID txId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant createdAt = Instant.now();

        Dispute d1 = Dispute.openDispute(
                disputeId,
                txId,
                userId,
                ReasonCode.FRAUD_SUSPECTED,
                "n",
                createdAt
        );

        when(delegate.findByUserId(userId)).thenReturn(Flux.just(d1));

        var sut = new CachedFindUserDisputesUseCase(delegate, cache, keys);

        StepVerifier.create(sut.findByUserId(userId))
                .expectNextMatches(d ->
                        d.disputeId().equals(disputeId)
                                && d.transactionId().equals(txId)
                                && d.userId().equals(userId)
                                && d.reasonCode() == ReasonCode.FRAUD_SUSPECTED
                                && d.note().equals("n")
                                && d.createdAt().toString().equals(createdAt.toString())
                )
                .verifyComplete();

        verify(delegate, times(1)).findByUserId(userId);
    }
}