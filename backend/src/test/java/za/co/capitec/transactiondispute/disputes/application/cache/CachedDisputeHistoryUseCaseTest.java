package za.co.capitec.transactiondispute.disputes.application.cache;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import za.co.capitec.transactiondispute.shared.infrastructure.cache.CacheKeyFactory;
import za.co.capitec.transactiondispute.shared.infrastructure.cache.CacheNames;
import za.co.capitec.transactiondispute.shared.infrastructure.cache.ReactiveCacheAside;

import java.util.UUID;

import static org.mockito.Mockito.*;

class CachedDisputeHistoryUseCaseTest {

    @Test
    void invalidateForUser_deletesCorrectKey_bestEffort() {
        var delegate = mock(za.co.capitec.transactiondispute.disputes.application.port.in.DisputeHistoryUseCase.class);
        ReactiveCacheAside cache = mock(ReactiveCacheAside.class);
        CacheKeyFactory keys = mock(CacheKeyFactory.class);

        UUID disputeId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        String key = "k1";
        when(keys.simple(CacheNames.DISPUTE_HISTORY_USER, userId.toString(), disputeId.toString())).thenReturn(key);
        when(cache.deleteBestEffort(key)).thenReturn(Mono.empty());

        CachedDisputeHistoryUseCase sut = new CachedDisputeHistoryUseCase(delegate, cache, keys);

        StepVerifier.create(sut.invalidateForUser(disputeId, userId))
                .verifyComplete();

        verify(cache, times(1)).deleteBestEffort(key);
    }

    @Test
    void invalidateForSupport_deletesCorrectKey_bestEffort() {
        var delegate = mock(za.co.capitec.transactiondispute.disputes.application.port.in.DisputeHistoryUseCase.class);
        ReactiveCacheAside cache = mock(ReactiveCacheAside.class);
        CacheKeyFactory keys = mock(CacheKeyFactory.class);

        UUID disputeId = UUID.randomUUID();

        String key = "k2";
        when(keys.simple(CacheNames.DISPUTE_HISTORY_SUPPORT, disputeId.toString())).thenReturn(key);
        when(cache.deleteBestEffort(key)).thenReturn(Mono.empty());

        CachedDisputeHistoryUseCase sut = new CachedDisputeHistoryUseCase(delegate, cache, keys);

        StepVerifier.create(sut.invalidateForSupport(disputeId))
                .verifyComplete();

        verify(cache, times(1)).deleteBestEffort(key);
    }
}