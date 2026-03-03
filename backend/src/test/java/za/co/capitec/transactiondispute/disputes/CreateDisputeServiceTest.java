package za.co.capitec.transactiondispute.disputes;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import za.co.capitec.transactiondispute.disputes.application.CreateDisputeService;
import za.co.capitec.transactiondispute.disputes.application.config.DisputesModuleProperties;
import za.co.capitec.transactiondispute.disputes.application.exceptions.*;
import za.co.capitec.transactiondispute.disputes.application.model.CreateDisputeCommand;
import za.co.capitec.transactiondispute.disputes.application.model.DisputableTransactionView;
import za.co.capitec.transactiondispute.disputes.application.port.out.DisputeEventPublisherPort;
import za.co.capitec.transactiondispute.disputes.application.port.out.DisputeHistoryRepositoryPort;
import za.co.capitec.transactiondispute.disputes.application.port.out.DisputeRepositoryPort;
import za.co.capitec.transactiondispute.disputes.application.port.out.TransactionQueryPort;
import za.co.capitec.transactiondispute.disputes.domain.model.Dispute;
import za.co.capitec.transactiondispute.disputes.domain.model.DisputeStatus;
import za.co.capitec.transactiondispute.disputes.domain.model.ReasonCode;
import za.co.capitec.transactiondispute.disputes.domain.model.TransactionStatus;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CreateDisputeServiceTest {

    private TransactionQueryPort transactions;
    private DisputeRepositoryPort disputes;
    private DisputeHistoryRepositoryPort history;
    private DisputeEventPublisherPort events;
    private TransactionalOperator txOperator;

    private Clock clock;
    private DisputesModuleProperties props;

    private CreateDisputeService service;

    @BeforeEach
    void setUp() {
        transactions = mock(TransactionQueryPort.class);
        disputes = mock(DisputeRepositoryPort.class);
        history = mock(DisputeHistoryRepositoryPort.class);
        events = mock(DisputeEventPublisherPort.class);
        txOperator = mock(TransactionalOperator.class);

        // transactional(..) should behave like identity for unit tests
        when(txOperator.transactional(ArgumentMatchers.<Mono<?>>any())).thenAnswer(inv -> inv.getArgument(0));

        when(txOperator.transactional(ArgumentMatchers.<Flux<?>>any())).thenAnswer(inv -> inv.getArgument(0));

        clock = Clock.fixed(Instant.parse("2026-02-26T08:00:00Z"), ZoneOffset.UTC);
        props = new DisputesModuleProperties(new DisputesModuleProperties.Rules(60));

        service = new CreateDisputeService(transactions, disputes, history, events, txOperator, clock, props);
    }

    @Test
    void create_whenTransactionNotFound_emitsTransactionNotFound() {
        var cmd = new CreateDisputeCommand(UUID.randomUUID(), UUID.randomUUID(), ReasonCode.FRAUD_SUSPECTED, "note");

        when(transactions.findDisputableById(cmd.transactionId())).thenReturn(Mono.empty());

        StepVerifier.create(service.create(cmd))
                .expectError(TransactionNotFoundException.class)
                .verify();

        verifyNoInteractions(disputes, history, events);
    }

    @Test
    void create_whenNotOwned_emitsTransactionNotOwned() {
        var userId = UUID.randomUUID();
        var txId = UUID.randomUUID();
        var cmd = new CreateDisputeCommand(userId, txId, ReasonCode.FRAUD_SUSPECTED, "note");

        when(transactions.findDisputableById(txId))
                .thenReturn(Mono.just(new DisputableTransactionView(
                        txId,
                        UUID.randomUUID(), // different owner
                        Instant.parse("2026-02-20T10:00:00Z"),
                        BigDecimal.TEN,
                        "ZAR",
                        "merchant",
                        TransactionStatus.POSTED
                )));

        StepVerifier.create(service.create(cmd))
                .expectError(TransactionNotOwnedException.class)
                .verify();

        verifyNoInteractions(disputes, history, events);
    }

    @Test
    void create_whenNotPosted_emitsNotDisputable() {
        var userId = UUID.randomUUID();
        var txId = UUID.randomUUID();
        var cmd = new CreateDisputeCommand(userId, txId, ReasonCode.FRAUD_SUSPECTED, "note");

        when(transactions.findDisputableById(txId))
                .thenReturn(Mono.just(new DisputableTransactionView(
                        txId,
                        userId,
                        Instant.parse("2026-02-20T10:00:00Z"),
                        BigDecimal.TEN,
                        "ZAR",
                        "merchant",
                        TransactionStatus.PENDING // not POSTED
                )));

        StepVerifier.create(service.create(cmd))
                .expectError(TransactionNotDisputableException.class)
                .verify();

        verifyNoInteractions(disputes, history, events);
    }

    @Test
    void create_whenWindowExpired_emitsWindowExpired() {
        var userId = UUID.randomUUID();
        var txId = UUID.randomUUID();
        var cmd = new CreateDisputeCommand(userId, txId, ReasonCode.FRAUD_SUSPECTED, "note");

        // older than 60 days from fixed clock (2026-02-26)
        when(transactions.findDisputableById(txId))
                .thenReturn(Mono.just(new DisputableTransactionView(
                        txId,
                        userId,
                        Instant.parse("2025-12-01T10:00:00Z"),
                        BigDecimal.TEN,
                        "ZAR",
                        "merchant",
                        TransactionStatus.POSTED
                )));

        StepVerifier.create(service.create(cmd))
                .expectError(TransactionDisputeWindowExpiredException.class)
                .verify();

        verifyNoInteractions(disputes, history, events);
    }

    @Test
    void create_whenOpenDisputeAlreadyExists_emitsOpenDisputeAlreadyExists() {
        var userId = UUID.randomUUID();
        var txId = UUID.randomUUID();
        var cmd = new CreateDisputeCommand(userId, txId, ReasonCode.FRAUD_SUSPECTED, "note");

        when(transactions.findDisputableById(txId))
                .thenReturn(Mono.just(new DisputableTransactionView(
                        txId,
                        userId,
                        Instant.parse("2026-02-20T10:00:00Z"),
                        BigDecimal.TEN,
                        "ZAR",
                        "merchant",
                        TransactionStatus.POSTED
                )));
        when(disputes.existsForTransactionAndUser(txId, userId)).thenReturn(Mono.just(true));

        StepVerifier.create(service.create(cmd))
                .expectError(OpenDisputeAlreadyExistsException.class)
                .verify();

        verify(disputes, never()).save(any());
        verifyNoInteractions(history, events);
    }

    @Test
    void create_whenUniqueConstraintTrips_mapsDuplicateKeyToOpenDisputeAlreadyExists() {
        var userId = UUID.randomUUID();
        var txId = UUID.randomUUID();
        var cmd = new CreateDisputeCommand(userId, txId, ReasonCode.FRAUD_SUSPECTED, "note");

        when(transactions.findDisputableById(txId))
                .thenReturn(Mono.just(new DisputableTransactionView(
                        txId,
                        userId,
                        Instant.parse("2026-02-20T10:00:00Z"),
                        BigDecimal.TEN,
                        "ZAR",
                        "merchant",
                        TransactionStatus.POSTED
                )));
        when(disputes.existsForTransactionAndUser(txId, userId)).thenReturn(Mono.just(false));
        when(disputes.save(any())).thenReturn(Mono.error(new DuplicateKeyException("dup")));
        // no need to stub history/events

        StepVerifier.create(service.create(cmd))
                .expectError(OpenDisputeAlreadyExistsException.class)
                .verify();

        verify(history, never()).append(any());
        verify(events, never()).publishDisputeCreated(any());
    }

    @Test
    void create_happyPath_persistsHistory_publishesEvent_andReturnsResult() {
        var userId = UUID.randomUUID();
        var txId = UUID.randomUUID();
        var cmd = new CreateDisputeCommand(userId, txId, ReasonCode.FRAUD_SUSPECTED, "note");

        when(transactions.findDisputableById(txId))
                .thenReturn(Mono.just(new DisputableTransactionView(
                        txId,
                        userId,
                        Instant.parse("2026-02-20T10:00:00Z"),
                        BigDecimal.TEN,
                        "ZAR",
                        "merchant",
                        TransactionStatus.POSTED
                )));
        when(disputes.existsForTransactionAndUser(txId, userId)).thenReturn(Mono.just(false));

        // capture the Dispute to assert persisted state, but return it as "saved"
        var disputeCaptor = ArgumentCaptor.forClass(Dispute.class);
        when(disputes.save(disputeCaptor.capture())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        when(history.append(any())).thenReturn(Mono.empty());
        when(events.publishDisputeCreated(any())).thenReturn(Mono.empty());

        StepVerifier.create(service.create(cmd))
                .assertNext(res -> {
                    // minimal assertions: result contains ids + OPEN status
                    org.assertj.core.api.Assertions.assertThat(res.transactionId()).isEqualTo(txId);
                    org.assertj.core.api.Assertions.assertThat(res.status()).isEqualTo(DisputeStatus.OPEN);
                    org.assertj.core.api.Assertions.assertThat(res.disputeId()).isNotNull();
                    org.assertj.core.api.Assertions.assertThat(res.createdAt()).isEqualTo(Instant.now(clock));
                })
                .verifyComplete();

        var saved = disputeCaptor.getValue();
        org.assertj.core.api.Assertions.assertThat(saved.transactionId()).isEqualTo(txId);
        org.assertj.core.api.Assertions.assertThat(saved.userId()).isEqualTo(userId);
        org.assertj.core.api.Assertions.assertThat(saved.disputeStatus()).isEqualTo(DisputeStatus.OPEN);

        verify(history, times(1)).append(any());
        verify(events, times(1)).publishDisputeCreated(any());
    }
}
