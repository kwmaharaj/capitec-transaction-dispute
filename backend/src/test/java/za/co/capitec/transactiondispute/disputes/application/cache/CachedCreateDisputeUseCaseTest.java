package za.co.capitec.transactiondispute.disputes.application.cache;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import za.co.capitec.transactiondispute.disputes.application.model.CreateDisputeCommand;
import za.co.capitec.transactiondispute.disputes.application.model.CreateDisputeResult;
import za.co.capitec.transactiondispute.disputes.application.port.in.CreateDisputeUseCase;
import za.co.capitec.transactiondispute.shared.application.port.out.TransactionCacheInvalidationPort;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CachedCreateDisputeUseCaseTest {

    @Test
    void create_success_invalidationBestEffort_stillReturnsResult() {
        CreateDisputeUseCase delegate = mock(CreateDisputeUseCase.class);
        CachedFindUserDisputesUseCase userDisputesCache = mock(CachedFindUserDisputesUseCase.class);
        CachedFindDisputesUseCase supportDisputesCache = mock(CachedFindDisputesUseCase.class);
        CachedDisputeViewQueryUseCase viewCache = mock(CachedDisputeViewQueryUseCase.class);
        CachedDisputeHistoryUseCase historyCache = mock(CachedDisputeHistoryUseCase.class);
        TransactionCacheInvalidationPort txCache = mock(TransactionCacheInvalidationPort.class);

        CreateDisputeCommand command = mock(CreateDisputeCommand.class);
        UUID userId = UUID.randomUUID();
        when(command.userId()).thenReturn(userId);

        CreateDisputeResult result = mock(CreateDisputeResult.class);
        UUID disputeId = UUID.randomUUID();
        UUID txId = UUID.randomUUID();
        when(result.disputeId()).thenReturn(disputeId);
        when(result.transactionId()).thenReturn(txId);

        when(delegate.create(command)).thenReturn(Mono.just(result));

        when(userDisputesCache.invalidate(userId)).thenReturn(Mono.empty());
        when(viewCache.invalidateUser(userId)).thenReturn(Mono.empty());
        when(viewCache.invalidateSupport()).thenReturn(Mono.empty());
        when(supportDisputesCache.invalidate()).thenReturn(Mono.empty());
        when(historyCache.invalidateForUser(disputeId, userId)).thenReturn(Mono.empty());
        when(historyCache.invalidateForSupport(disputeId)).thenReturn(Mono.empty());
        when(txCache.invalidateAfterTransactionChange(txId, userId)).thenReturn(Mono.empty());

        CachedCreateDisputeUseCase sut = new CachedCreateDisputeUseCase(
                delegate, userDisputesCache, supportDisputesCache, viewCache, historyCache, txCache
        );

        StepVerifier.create(sut.create(command))
                .expectNext(result)
                .verifyComplete();

        verify(delegate, times(1)).create(command);

        verify(userDisputesCache, times(1)).invalidate(userId);
        verify(viewCache, times(1)).invalidateUser(userId);
        verify(viewCache, times(1)).invalidateSupport();
        verify(supportDisputesCache, times(1)).invalidate();
        verify(historyCache, times(1)).invalidateForUser(disputeId, userId);
        verify(historyCache, times(1)).invalidateForSupport(disputeId);
        verify(txCache, times(1)).invalidateAfterTransactionChange(txId, userId);
    }

    @Test
    void create_invalidationFails_failOpenStillReturnsResult() {
        CreateDisputeUseCase delegate = mock(CreateDisputeUseCase.class);
        CachedFindUserDisputesUseCase userDisputesCache = mock(CachedFindUserDisputesUseCase.class);
        CachedFindDisputesUseCase supportDisputesCache = mock(CachedFindDisputesUseCase.class);
        CachedDisputeViewQueryUseCase viewCache = mock(CachedDisputeViewQueryUseCase.class);
        CachedDisputeHistoryUseCase historyCache = mock(CachedDisputeHistoryUseCase.class);
        TransactionCacheInvalidationPort txCache = mock(TransactionCacheInvalidationPort.class);

        CreateDisputeCommand command = mock(CreateDisputeCommand.class);
        UUID userId = UUID.randomUUID();
        when(command.userId()).thenReturn(userId);

        CreateDisputeResult result = mock(CreateDisputeResult.class);
        UUID disputeId = UUID.randomUUID();
        UUID txId = UUID.randomUUID();
        when(result.disputeId()).thenReturn(disputeId);
        when(result.transactionId()).thenReturn(txId);

        when(delegate.create(command)).thenReturn(Mono.just(result));

        // Fail early in invalidation chain
        when(userDisputesCache.invalidate(userId)).thenReturn(Mono.error(new RuntimeException("redis down")));
        when(viewCache.invalidateUser(any())).thenReturn(Mono.empty());
        when(viewCache.invalidateSupport()).thenReturn(Mono.empty());
        when(supportDisputesCache.invalidate()).thenReturn(Mono.empty());
        when(historyCache.invalidateForUser(any(), any())).thenReturn(Mono.empty());
        when(historyCache.invalidateForSupport(any())).thenReturn(Mono.empty());
        when(txCache.invalidateAfterTransactionChange(any(), any())).thenReturn(Mono.empty());

        CachedCreateDisputeUseCase sut = new CachedCreateDisputeUseCase(
                delegate, userDisputesCache, supportDisputesCache, viewCache, historyCache, txCache
        );

        StepVerifier.create(sut.create(command))
                .expectNext(result)
                .verifyComplete();

        verify(delegate, times(1)).create(command);
        verify(userDisputesCache, times(1)).invalidate(userId);
    }
}