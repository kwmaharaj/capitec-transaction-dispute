package za.co.capitec.transactiondispute.disputes.application.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import za.co.capitec.transactiondispute.disputes.application.port.in.FindDisputesUseCase;
import za.co.capitec.transactiondispute.disputes.domain.model.Dispute;
import za.co.capitec.transactiondispute.disputes.domain.model.DisputeStatus;
import za.co.capitec.transactiondispute.shared.infrastructure.cache.CacheKeyFactory;
import za.co.capitec.transactiondispute.shared.infrastructure.cache.CacheNames;
import za.co.capitec.transactiondispute.shared.infrastructure.cache.ReactiveCacheAside;

import java.util.function.Supplier;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CachedFindDisputesUseCaseTest {

    @Test
    void findByDisputeStatus_cacheHit_returnsCachedValue_delegateNotInvoked() {
        FindDisputesUseCase delegate = mock(FindDisputesUseCase.class);
        ReactiveCacheAside cache = mock(ReactiveCacheAside.class);
        CacheKeyFactory keys = mock(CacheKeyFactory.class);

        DisputeStatus status = DisputeStatus.OPEN;
        Pageable pageable = mock(Pageable.class);

        when(keys.hash(any())).thenReturn("h1");
        when(keys.versioned(CacheNames.DISPUTES_SUPPORT, "h1")).thenReturn(Mono.just("k1"));

        @SuppressWarnings("unchecked")
        Page<Dispute> cachedPage = (Page<Dispute>) mock(Page.class);

        when(cache.getOrLoad(
                eq(CacheNames.DISPUTES_SUPPORT),
                any(Mono.class),
                any(TypeReference.class),
                any()
        )).thenReturn(Mono.just(cachedPage));

        CachedFindDisputesUseCase sut = new CachedFindDisputesUseCase(delegate, cache, keys);

        StepVerifier.create(sut.findByDisputeStatus(status, pageable))
                .expectNext(cachedPage)
                .verifyComplete();

        verify(delegate, never()).findByDisputeStatus(any(), any());
        verify(keys, times(1)).hash(any());
        verify(keys, times(1)).versioned(CacheNames.DISPUTES_SUPPORT, "h1");
        verify(cache, times(1)).getOrLoad(eq(CacheNames.DISPUTES_SUPPORT), any(Mono.class), any(TypeReference.class), any());
    }

    @Test
    void findByDisputeStatus_cacheMiss_invokesDelegate_viaLoader() {
        FindDisputesUseCase delegate = mock(FindDisputesUseCase.class);
        ReactiveCacheAside cache = mock(ReactiveCacheAside.class);
        CacheKeyFactory keys = mock(CacheKeyFactory.class);

        DisputeStatus status = DisputeStatus.OPEN;
        Pageable pageable = mock(Pageable.class);

        when(keys.hash(any())).thenReturn("h2");
        when(keys.versioned(CacheNames.DISPUTES_SUPPORT, "h2")).thenReturn(Mono.just("k2"));

        @SuppressWarnings("unchecked")
        Page<Dispute> delegatePage = (Page<Dispute>) mock(Page.class);

        when(delegate.findByDisputeStatus(status, pageable)).thenReturn(Mono.just(delegatePage));

        // Simulate "cache miss" by having the cache call the supplied loader.
        when(cache.getOrLoad(
                eq(CacheNames.DISPUTES_SUPPORT),
                any(Mono.class),
                any(TypeReference.class),
                any()
        )).thenAnswer(inv -> {
            @SuppressWarnings("unchecked")
            Supplier<Mono<Page<Dispute>>> loader = (Supplier<Mono<Page<Dispute>>>) inv.getArgument(3);
            return loader.get();
        });

        CachedFindDisputesUseCase sut = new CachedFindDisputesUseCase(delegate, cache, keys);

        StepVerifier.create(sut.findByDisputeStatus(status, pageable))
                .expectNext(delegatePage)
                .verifyComplete();

        verify(delegate, times(1)).findByDisputeStatus(status, pageable);
        verify(keys, times(1)).hash(any());
        verify(keys, times(1)).versioned(CacheNames.DISPUTES_SUPPORT, "h2");
        verify(cache, times(1)).getOrLoad(eq(CacheNames.DISPUTES_SUPPORT), any(Mono.class), any(TypeReference.class), any());
    }

    @Test
    void invalidate_bumpsNamespaceVersion() {
        FindDisputesUseCase delegate = mock(FindDisputesUseCase.class);
        ReactiveCacheAside cache = mock(ReactiveCacheAside.class);
        CacheKeyFactory keys = mock(CacheKeyFactory.class);

        when(keys.bumpNamespaceVersion(CacheNames.DISPUTES_SUPPORT)).thenReturn(Mono.just(1L));

        CachedFindDisputesUseCase sut = new CachedFindDisputesUseCase(delegate, cache, keys);

        StepVerifier.create(sut.invalidate())
                .verifyComplete();

        verify(keys, times(1)).bumpNamespaceVersion(CacheNames.DISPUTES_SUPPORT);
        verifyNoInteractions(cache);
        verifyNoInteractions(delegate);
    }
}