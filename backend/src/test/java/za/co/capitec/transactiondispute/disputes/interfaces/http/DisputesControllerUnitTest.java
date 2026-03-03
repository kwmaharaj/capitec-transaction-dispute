package za.co.capitec.transactiondispute.disputes.interfaces.http;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import za.co.capitec.transactiondispute.disputes.application.model.*;
import za.co.capitec.transactiondispute.disputes.application.port.in.*;
import za.co.capitec.transactiondispute.disputes.domain.model.*;
import za.co.capitec.transactiondispute.disputes.infrastructure.persistence.DisputeActor;
import za.co.capitec.transactiondispute.shared.interfaces.http.ApiResponse;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class DisputesControllerUnitTest {

    private CreateDisputeUseCase createDispute;
    private FindUserDisputesUseCase findUserDisputesUseCase;
    private FindDisputesUseCase findDisputesUseCase;
    private DecideDisputeUseCase decideDisputeUseCase;
    private DisputeViewQueryUseCase disputeViewQueryUseCase;
    private DisputeHistoryUseCase disputeHistoryUseCase;

    private DisputesController controller;

    @BeforeEach
    void setUp() {
        createDispute = mock(CreateDisputeUseCase.class);
        findUserDisputesUseCase = mock(FindUserDisputesUseCase.class);
        findDisputesUseCase = mock(FindDisputesUseCase.class);
        decideDisputeUseCase = mock(DecideDisputeUseCase.class);
        disputeViewQueryUseCase = mock(DisputeViewQueryUseCase.class);
        disputeHistoryUseCase = mock(DisputeHistoryUseCase.class);

        controller = new DisputesController(
                createDispute,
                findUserDisputesUseCase,
                findDisputesUseCase,
                decideDisputeUseCase,
                disputeViewQueryUseCase,
                disputeHistoryUseCase
        );
    }

    @Test
    void create_mapsAuthNameToUserId_andReturnsApiResponseOk() {
        var auth = mock(org.springframework.security.core.Authentication.class);
        var userId = UUID.randomUUID();
        when(auth.getName()).thenReturn(userId.toString());

        var txId = UUID.randomUUID();
        var disputeId = UUID.randomUUID();
        var req = new CreateDisputeRequest(txId, ReasonCode.FRAUD_SUSPECTED, "note");

        var result = new CreateDisputeResult(txId, disputeId, DisputeStatus.OPEN, Instant.parse("2026-02-01T00:00:00Z"));
        when(createDispute.create(any())).thenReturn(Mono.just(result));

        StepVerifier.create(controller.create(auth, req))
                .assertNext(resp -> {
                    assertThat(resp).isEqualTo(ApiResponse.ok(result));
                })
                .verifyComplete();

        var captor = ArgumentCaptor.forClass(CreateDisputeCommand.class);
        verify(createDispute).create(captor.capture());
        assertThat(captor.getValue().userId()).isEqualTo(userId);
        assertThat(captor.getValue().transactionId()).isEqualTo(txId);
        assertThat(captor.getValue().reasonCode()).isEqualTo(ReasonCode.FRAUD_SUSPECTED);
        assertThat(captor.getValue().note()).isEqualTo("note");
    }

    @Test
    void getUserDisputes_mapsDomainToResponseList() {
        var auth = mock(org.springframework.security.core.Authentication.class);
        var userId = UUID.randomUUID();
        when(auth.getName()).thenReturn(userId.toString());

        var dispute = Dispute.openDispute(
                UUID.randomUUID(),
                UUID.randomUUID(),
                userId,
                ReasonCode.FRAUD_SUSPECTED,
                "note",
                Instant.parse("2026-02-01T00:00:00Z")
        );

        when(findUserDisputesUseCase.findByUserId(userId)).thenReturn(Flux.just(dispute));

        StepVerifier.create(controller.getUserDisputes(auth))
                .assertNext(resp -> {
                    assertThat(resp.success()).isTrue();
                    assertThat(resp.data()).hasSize(1);
                    assertThat(resp.data().get(0).disputeId()).isEqualTo(dispute.disputeId());
                    assertThat(resp.data().get(0).transactionId()).isEqualTo(dispute.transactionId());
                    assertThat(resp.data().get(0).disputeStatus()).isEqualTo(dispute.disputeStatus());
                })
                .verifyComplete();
    }

    @Test
    void getUserDisputesView_buildsCriteriaAndPageable_andMapsRowsToResponses() {
        var auth = mock(org.springframework.security.core.Authentication.class);
        var userId = UUID.randomUUID();
        when(auth.getName()).thenReturn(userId.toString());

        var row = new DisputeTransactionViewRow(
                UUID.randomUUID(), UUID.randomUUID(), userId,
                za.co.capitec.transactiondispute.disputes.domain.model.DisputeStatus.OPEN,
                "FRAUD", "note",
                Instant.parse("2026-02-01T00:00:00Z"),
                Instant.parse("2026-01-31T00:00:00Z"),
                BigDecimal.TEN, "ZAR", "Merchant",
                za.co.capitec.transactiondispute.disputes.domain.model.TransactionStatus.POSTED,
                "123456"
        );

        var pageReq = PageRequest.of(0, 20);
        when(disputeViewQueryUseCase.searchForUser(eq(userId), any(), any()))
                .thenReturn(Mono.just(new PageImpl<>(List.of(row), pageReq, 1)));

        StepVerifier.create(controller.getUserDisputesView(
                        auth,
                        null, null, null, null, null,
                        za.co.capitec.transactiondispute.disputes.domain.model.DisputeStatus.OPEN,
                        za.co.capitec.transactiondispute.disputes.domain.model.TransactionStatus.POSTED,
                        null, null,
                        0, 20
                ))
                .assertNext(resp -> {
                    assertThat(resp.success()).isTrue();
                    var page = resp.data();
                    assertThat(page.items()).hasSize(1);
                    var item = page.items().get(0);
                    assertThat(item.disputeId()).isEqualTo(row.disputeId());
                    assertThat(item.transactionId()).isEqualTo(row.transactionId());
                    assertThat(item.userId()).isEqualTo(row.userId());
                    assertThat(item.rrn()).isEqualTo(row.rrn());
                })
                .verifyComplete();

        var criteriaCaptor = ArgumentCaptor.forClass(DisputeSearchCriteria.class);
        var pageableCaptor = ArgumentCaptor.forClass(org.springframework.data.domain.Pageable.class);
        verify(disputeViewQueryUseCase).searchForUser(eq(userId), criteriaCaptor.capture(), pageableCaptor.capture());

        assertThat(criteriaCaptor.getValue().disputeStatus())
                .isEqualTo(za.co.capitec.transactiondispute.disputes.domain.model.DisputeStatus.OPEN);
        assertThat(criteriaCaptor.getValue().transactionStatus())
                .isEqualTo(za.co.capitec.transactiondispute.disputes.domain.model.TransactionStatus.POSTED);
        assertThat(pageableCaptor.getValue().getPageNumber()).isZero();
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(20);
    }

    @Test
    void getDisputeHistoryForUser_mapsHistoryItems() {
        var auth = mock(org.springframework.security.core.Authentication.class);
        var userId = UUID.randomUUID();
        when(auth.getName()).thenReturn(userId.toString());

        var disputeId = UUID.randomUUID();

        var item = new DisputeHistoryItem(
                UUID.randomUUID(),
                disputeId,
                za.co.capitec.transactiondispute.disputes.domain.model.DisputeStatus.OPEN,
                za.co.capitec.transactiondispute.disputes.domain.model.DisputeStatus.IN_PROGRESS,
                "note",
                new DisputeActor("SUPPORT", userId),
                Instant.parse("2026-02-02T00:00:00Z")
        );

        when(disputeHistoryUseCase.findForUser(disputeId, userId)).thenReturn(Flux.just(item));

        StepVerifier.create(controller.getDisputeHistoryForUser(auth, disputeId))
                .assertNext(resp -> {
                    assertThat(resp.success()).isTrue();
                    assertThat(resp.data()).hasSize(1);
                    assertThat(resp.data().get(0).historyId()).isEqualTo(item.historyId());
                    assertThat(resp.data().get(0).disputeActor()).isEqualTo(item.disputeActor());
                })
                .verifyComplete();
    }
}