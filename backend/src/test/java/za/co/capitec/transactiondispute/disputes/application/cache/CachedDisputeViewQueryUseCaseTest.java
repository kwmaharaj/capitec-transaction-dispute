package za.co.capitec.transactiondispute.disputes.application.cache;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import za.co.capitec.transactiondispute.shared.infrastructure.cache.CacheKeyFactory;
import za.co.capitec.transactiondispute.shared.infrastructure.cache.CacheNames;

import static org.mockito.Mockito.*;

class CachedDisputeViewQueryUseCaseTest {

    @Test
    void invalidateUser_bumpsCorrectNamespace() {
        var delegate = mock(za.co.capitec.transactiondispute.disputes.application.port.in.DisputeViewQueryUseCase.class);
        var cache = mock(za.co.capitec.transactiondispute.shared.infrastructure.cache.ReactiveCacheAside.class);
        CacheKeyFactory keys = mock(CacheKeyFactory.class);

        var sut = new CachedDisputeViewQueryUseCase(delegate, cache, keys);

        var userId = java.util.UUID.randomUUID();
        String ns = CacheNames.DISPUTE_VIEW_USER + ":" + userId;

        when(keys.bumpNamespaceVersion(ns)).thenReturn(Mono.just(1L));

        StepVerifier.create(sut.invalidateUser(userId)).verifyComplete();

        verify(keys, times(1)).bumpNamespaceVersion(ns);
    }

    @Test
    void invalidateSupport_bumpsCorrectNamespace() {
        var delegate = mock(za.co.capitec.transactiondispute.disputes.application.port.in.DisputeViewQueryUseCase.class);
        var cache = mock(za.co.capitec.transactiondispute.shared.infrastructure.cache.ReactiveCacheAside.class);
        CacheKeyFactory keys = mock(CacheKeyFactory.class);

        var sut = new CachedDisputeViewQueryUseCase(delegate, cache, keys);

        when(keys.bumpNamespaceVersion(CacheNames.DISPUTE_VIEW_SUPPORT)).thenReturn(Mono.just(1L));

        StepVerifier.create(sut.invalidateSupport()).verifyComplete();

        verify(keys, times(1)).bumpNamespaceVersion(CacheNames.DISPUTE_VIEW_SUPPORT);
    }
}