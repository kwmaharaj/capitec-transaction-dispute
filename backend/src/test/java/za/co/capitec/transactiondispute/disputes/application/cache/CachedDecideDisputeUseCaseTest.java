package za.co.capitec.transactiondispute.disputes.application.cache;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import za.co.capitec.transactiondispute.disputes.application.port.in.DecideDisputeUseCase;
import za.co.capitec.transactiondispute.disputes.domain.model.Dispute;
import za.co.capitec.transactiondispute.disputes.domain.model.DisputeStatus;
import za.co.capitec.transactiondispute.shared.application.port.out.TransactionCacheInvalidationPort;

import java.util.UUID;

import static org.mockito.Mockito.*;

class CachedDecideDisputeUseCaseTest {

    @Test
    void decide_success_invalidationBestEffort_stillReturnsSavedDispute() {
        DecideDisputeUseCase delegate = mock(DecideDisputeUseCase.class);
        CachedFindUserDisputesUseCase userDisputesCache = mock(CachedFindUserDisputesUseCase.class);
        CachedFindDisputesUseCase supportDisputesCache = mock(CachedFindDisputesUseCase.class);
        CachedDisputeViewQueryUseCase viewCache = mock(CachedDisputeViewQueryUseCase.class);
        CachedDisputeHistoryUseCase historyCache = mock(CachedDisputeHistoryUseCase.class);
        TransactionCacheInvalidationPort txCache = mock(TransactionCacheInvalidationPort.class);

        UUID disputeId = UUID.randomUUID();
        UUID supportUserId = UUID.randomUUID();
        DisputeStatus status = DisputeStatus.RESOLVED;
        String note = "ok";

        Dispute saved = mock(Dispute.class);
        UUID userId = UUID.randomUUID();
        UUID txId = UUID.randomUUID();
        when(saved.userId()).thenReturn(userId);
        when(saved.disputeId()).thenReturn(disputeId);
        when(saved.transactionId()).thenReturn(txId);

        when(delegate.decide(disputeId, supportUserId, status, note)).thenReturn(Mono.just(saved));

        when(userDisputesCache.invalidate(userId)).thenReturn(Mono.empty());
        when(viewCache.invalidateUser(userId)).thenReturn(Mono.empty());
        when(viewCache.invalidateSupport()).thenReturn(Mono.empty());
        when(supportDisputesCache.invalidate()).thenReturn(Mono.empty());
        when(historyCache.invalidateForUser(disputeId, userId)).thenReturn(Mono.empty());
        when(historyCache.invalidateForSupport(disputeId)).thenReturn(Mono.empty());
        when(txCache.invalidateAfterTransactionChange(txId, userId)).thenReturn(Mono.empty());

        CachedDecideDisputeUseCase sut = new CachedDecideDisputeUseCase(
                delegate, userDisputesCache, supportDisputesCache, viewCache, historyCache, txCache
        );

        StepVerifier.create(sut.decide(disputeId, supportUserId, status, note))
                .expectNext(saved)
                .verifyComplete();

        verify(delegate, times(1)).decide(disputeId, supportUserId, status, note);

        verify(userDisputesCache, times(1)).invalidate(userId);
        verify(viewCache, times(1)).invalidateUser(userId);
        verify(viewCache, times(1)).invalidateSupport();
        verify(supportDisputesCache, times(1)).invalidate();
        verify(historyCache, times(1)).invalidateForUser(disputeId, userId);
        verify(historyCache, times(1)).invalidateForSupport(disputeId);
        verify(txCache, times(1)).invalidateAfterTransactionChange(txId, userId);
    }

    @Test
    void decide_invalidationFails_failOpenStillReturnsSavedDispute() {
        DecideDisputeUseCase delegate = mock(DecideDisputeUseCase.class);
        CachedFindUserDisputesUseCase userDisputesCache = mock(CachedFindUserDisputesUseCase.class);
        CachedFindDisputesUseCase supportDisputesCache = mock(CachedFindDisputesUseCase.class);
        CachedDisputeViewQueryUseCase viewCache = mock(CachedDisputeViewQueryUseCase.class);
        CachedDisputeHistoryUseCase historyCache = mock(CachedDisputeHistoryUseCase.class);
        TransactionCacheInvalidationPort txCache = mock(TransactionCacheInvalidationPort.class);

        UUID disputeId = UUID.randomUUID();
        UUID supportUserId = UUID.randomUUID();
        DisputeStatus status = DisputeStatus.REJECTED;
        String note = "no";

        Dispute saved = mock(Dispute.class);
        UUID userId = UUID.randomUUID();
        UUID txId = UUID.randomUUID();

        when(saved.userId()).thenReturn(userId);
        when(saved.disputeId()).thenReturn(disputeId);
        when(saved.transactionId()).thenReturn(txId);

        when(delegate.decide(disputeId, supportUserId, status, note)).thenReturn(Mono.just(saved));

        // one invalidation fails (fail-open expected)
        when(userDisputesCache.invalidate(userId)).thenReturn(Mono.error(new RuntimeException("redis down")));

        // all others MUST return non-null Monos (otherwise NPE: last)
        when(viewCache.invalidateUser(userId)).thenReturn(Mono.empty());
        when(viewCache.invalidateSupport()).thenReturn(Mono.empty());
        when(supportDisputesCache.invalidate()).thenReturn(Mono.empty());
        when(historyCache.invalidateForUser(disputeId, userId)).thenReturn(Mono.empty());
        when(historyCache.invalidateForSupport(disputeId)).thenReturn(Mono.empty());
        when(txCache.invalidateAfterTransactionChange(txId, userId)).thenReturn(Mono.empty());

        CachedDecideDisputeUseCase sut = new CachedDecideDisputeUseCase(
                delegate, userDisputesCache, supportDisputesCache, viewCache, historyCache, txCache
        );

        StepVerifier.create(sut.decide(disputeId, supportUserId, status, note))
                .expectNext(saved)
                .verifyComplete();

        verify(delegate, times(1)).decide(disputeId, supportUserId, status, note);
    }
}