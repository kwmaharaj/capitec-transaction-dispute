package za.co.capitec.transactiondispute.disputes.interfaces.http;

import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import za.co.capitec.transactiondispute.disputes.application.model.*;
import za.co.capitec.transactiondispute.disputes.application.port.in.*;
import za.co.capitec.transactiondispute.disputes.domain.model.Dispute;
import za.co.capitec.transactiondispute.disputes.domain.model.DisputeStatus;
import za.co.capitec.transactiondispute.disputes.domain.model.TransactionStatus;
import za.co.capitec.transactiondispute.disputes.infrastructure.persistence.DisputeActor;
import za.co.capitec.transactiondispute.shared.interfaces.http.ApiResponse;
import za.co.capitec.transactiondispute.shared.interfaces.http.PageResponse;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1")
public class DisputesController {

    private final CreateDisputeUseCase createDispute;
    private final FindUserDisputesUseCase findUserDisputesUseCase;
    private final FindDisputesUseCase findDisputesUseCase;
    private final DecideDisputeUseCase decideDisputeUseCase;
    private final DisputeViewQueryUseCase disputeViewQueryUseCase;
    private final DisputeHistoryUseCase disputeHistoryUseCase;

    public DisputesController(CreateDisputeUseCase createDispute,
                              FindUserDisputesUseCase findUserDisputesUseCase,
                              FindDisputesUseCase findDisputesUseCase,
                              DecideDisputeUseCase decideDisputeUseCase,
                              DisputeViewQueryUseCase disputeViewQueryUseCase,
                              DisputeHistoryUseCase disputeHistoryUseCase) {
        this.createDispute = createDispute;
        this.findUserDisputesUseCase=findUserDisputesUseCase;
        this.findDisputesUseCase =findDisputesUseCase;
        this.decideDisputeUseCase=decideDisputeUseCase;
        this.disputeViewQueryUseCase = disputeViewQueryUseCase;
        this.disputeHistoryUseCase = disputeHistoryUseCase;
    }

    @PostMapping("/disputes")
    public Mono<ApiResponse<CreateDisputeResult>> create(Authentication auth, @Valid @RequestBody CreateDisputeRequest req) {
        UUID userId = UUID.fromString(auth.getName());
        return createDispute.create(new CreateDisputeCommand(
                userId,
                req.transactionId(),
                req.reasonCode(),
                req.note()
        )).map(ApiResponse::ok);
    }

    @GetMapping("/disputes")
    public Mono<ApiResponse<List<DisputeResponse>>>  getUserDisputes(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        return findUserDisputesUseCase.findByUserId(userId)
                .map(DisputeResponse::from)
                .collectList()
                .map(ApiResponse::ok);
    }

    /**
     * user disputes view: includes transaction summary and supports filtering/paging.
     */
    @GetMapping("/disputes/view")
    public Mono<ApiResponse<PageResponse<DisputeTransactionViewResponse>>> getUserDisputesView(
            Authentication authentication,
            @RequestParam(required = false) UUID disputeId,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount,
            @RequestParam(required = false) String rrn,
            @RequestParam(required = false) String merchant,
            @RequestParam(required = false) DisputeStatus disputeStatus,
            @RequestParam(required = false) TransactionStatus transactionStatus,
            @RequestParam(required = false) Instant fromDate,
            @RequestParam(required = false) Instant toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        UUID userId = UUID.fromString(authentication.getName());

        var criteria = new DisputeSearchCriteria(
                disputeId,
                minAmount,
                maxAmount,
                rrn,
                merchant,
                disputeStatus,
                transactionStatus,
                fromDate,
                toDate
        );

        var pageable = PageRequest.of(page, size);
        return disputeViewQueryUseCase.searchForUser(userId, criteria, pageable)
                .map(p -> p.map(DisputeTransactionViewResponse::from))
                .map(PageResponse::from)
                .map(ApiResponse::ok);
    }

    @GetMapping("/disputes/{disputeId}/history")
    public Mono<ApiResponse<List<DisputeHistoryResponse>>> getDisputeHistoryForUser(
            Authentication authentication,
            @PathVariable UUID disputeId
    ) {
        UUID userId = UUID.fromString(authentication.getName());
        return disputeHistoryUseCase.findForUser(disputeId, userId)
                .map(DisputeHistoryResponse::from)
                .collectList()
                .map(ApiResponse::ok);
    }

    @GetMapping("/support/disputes")
    public Mono<ApiResponse<PageResponse<DisputeResponse>>>  getDisputes(
            @RequestParam(required = false) DisputeStatus disputeStatus,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        var pageable = PageRequest.of(page, size);
        return findDisputesUseCase.findByDisputeStatus(disputeStatus, pageable)
                .map(p -> p.map(DisputeResponse::from))
                .map(PageResponse::from)
                .map(ApiResponse::ok);
    }

    /**
     * Production-grade support disputes view: includes transaction summary and supports filtering/paging.
     */
    @GetMapping("/support/disputes/view")
    public Mono<ApiResponse<PageResponse<DisputeTransactionViewResponse>>>  getSupportDisputesView(
            @RequestParam(required = false) UUID disputeId,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount,
            @RequestParam(required = false) String rrn,
            @RequestParam(required = false) String merchant,
            @RequestParam(required = false) DisputeStatus disputeStatus,
            @RequestParam(required = false) TransactionStatus transactionStatus,
            @RequestParam(required = false) Instant fromDate,
            @RequestParam(required = false) Instant toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        var criteria = new DisputeSearchCriteria(
                disputeId,
                minAmount,
                maxAmount,
                rrn,
                merchant,
                disputeStatus,
                transactionStatus,
                fromDate,
                toDate
        );

        var pageable = PageRequest.of(page, size);
        return disputeViewQueryUseCase.searchForSupport(criteria, pageable)
                .map(p -> p.map(DisputeTransactionViewResponse::from))
                .map(PageResponse::from)
                .map(ApiResponse::ok);
    }

    @GetMapping("/support/disputes/{disputeId}/history")
    public Mono<ApiResponse<List<DisputeHistoryResponse>>> getDisputeHistoryForSupport(@PathVariable UUID disputeId) {
        return disputeHistoryUseCase.findForSupport(disputeId)
                .map(DisputeHistoryResponse::from)
                .collectList()
                .map(ApiResponse::ok);
    }


    @PostMapping("/support/disputes/{disputeId}/decision")
    public Mono<ApiResponse<DisputeResponse>> decideDispute(
            @PathVariable UUID disputeId,
            @RequestBody DisputeDecisionRequest request,
            Authentication authentication
    ) {
        UUID supportUserId = UUID.fromString(authentication.getName());
        return decideDisputeUseCase.decide(disputeId, supportUserId, request.disputeStatus(), request.supportNote())
                .map(DisputeResponse::from)
                .map(ApiResponse::ok);
    }

    public record DisputeResponse(
            UUID disputeId,
            UUID transactionId,
            DisputeStatus disputeStatus,
            String reasonCode,
            String note,
            Instant createdAt
    ) {

        public static DisputeResponse from(Dispute dispute) {
            return new DisputeResponse(
                    dispute.disputeId(),
                    dispute.transactionId(),
                    dispute.disputeStatus(),
                    dispute.reasonCode().name(),
                    dispute.note(),
                    dispute.createdAt()
            );
        }
    }

    public record DisputeTransactionViewResponse(
            UUID disputeId,
            UUID transactionId,
            UUID userId,
            DisputeStatus disputeStatus,
            String reasonCode,
            String disputeNote,
            Instant disputeCreatedAt,
            Instant postedAt,
            BigDecimal amount,
            String currency,
            String merchant,
            TransactionStatus transactionStatus,
            String rrn
    ) {
        public static DisputeTransactionViewResponse from(DisputeTransactionViewRow r) {
            return new DisputeTransactionViewResponse(
                    r.disputeId(),
                    r.transactionId(),
                    r.userId(),
                    r.disputeStatus(),
                    r.reasonCode(),
                    r.disputeNote(),
                    r.createdAt(),
                    r.postedAt(),
                    r.amount(),
                    r.currency(),
                    r.merchant(),
                    r.transactionStatus(),
                    r.rrn()
            );
        }
    }

    public record DisputeHistoryResponse(
            UUID historyId,
            UUID disputeId,
            DisputeStatus fromDisputeStatus,
            DisputeStatus toDisputeStatus,
            String note,
            DisputeActor disputeActor,
            Instant changedAt
    ) {
        public static DisputeHistoryResponse from(DisputeHistoryItem i) {
            return new DisputeHistoryResponse(
                    i.historyId(),
                    i.disputeId(),
                    i.fromDisputeStatus(),
                    i.toDisputeStatus(),
                    i.note(),
                    i.disputeActor(),
                    i.changedAt()
            );
        }
    }
}
