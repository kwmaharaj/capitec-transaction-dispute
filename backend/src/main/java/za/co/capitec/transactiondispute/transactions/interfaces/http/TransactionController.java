package za.co.capitec.transactiondispute.transactions.interfaces.http;

import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import za.co.capitec.transactiondispute.shared.interfaces.http.ApiResponse;
import za.co.capitec.transactiondispute.shared.interfaces.http.PageResponse;
import za.co.capitec.transactiondispute.transactions.application.exceptions.TransactionNotFoundException;
import za.co.capitec.transactiondispute.transactions.application.model.TransactionSearchCriteria;
import za.co.capitec.transactiondispute.transactions.application.model.TransactionView;
import za.co.capitec.transactiondispute.transactions.application.port.in.TransactionQueryUseCase;
import za.co.capitec.transactiondispute.transactions.domain.model.TransactionStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/v1")
public class TransactionController {

    private final TransactionQueryUseCase queries;

    public TransactionController(TransactionQueryUseCase queries) {
        this.queries = queries;
    }

    /**
     * Production-grade: server-side filtering + paging. Returns only caller-owned transactions.
     */
    @GetMapping("/transactions")
    public Mono<ApiResponse<PageResponse<TransactionView>>> searchForUser(
            Authentication auth,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount,
            @RequestParam(required = false) String rrn,
            @RequestParam(required = false) String merchant,
            @RequestParam(required = false) TransactionStatus transactionStatus,
            @RequestParam(required = false) Instant fromDate,
            @RequestParam(required = false) Instant toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        UUID userId = UUID.fromString(auth.getName());

        var criteria = new TransactionSearchCriteria(
                minAmount,
                maxAmount,
                rrn,
                merchant,
                transactionStatus,
                fromDate,
                toDate
        );

        var pageable = PageRequest.of(page, size);
        return queries.searchForUser(userId, criteria, pageable)
                .map(PageResponse::from)
                .map(ApiResponse::ok);
    }

    /**
     * Objective #3: enforce ownership for USER; allow SUPPORT to fetch any transaction.
     * We return 404 for both "not found" and "not owned".
     */
    @GetMapping("/transactions/{transactionId}")
    public Mono<ApiResponse<TransactionView>>  findById(Authentication auth, @PathVariable UUID transactionId) {

        boolean isSupport = auth.getAuthorities().stream().anyMatch(a -> "ROLE_SUPPORT".equals(a.getAuthority()));

        if (isSupport) {
            return queries.findById(transactionId)
                    .switchIfEmpty(Mono.error(new TransactionNotFoundException(transactionId)))
                    .map(ApiResponse::ok);
        }

        UUID userId = UUID.fromString(auth.getName());
        return queries.findById(transactionId)
                .filter(tx -> userId.equals(tx.userId()))
                .switchIfEmpty(Mono.error(new TransactionNotFoundException(transactionId)))
                .map(ApiResponse::ok);
    }

    /**
     * Support: server-side filtering + paging across all transactions.
     */
    @GetMapping("/support/transactions")
    public Mono<ApiResponse<PageResponse<TransactionView>>> searchAllForSupport(
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount,
            @RequestParam(required = false) String rrn,
            @RequestParam(required = false) String merchant,
            @RequestParam(required = false) TransactionStatus transactionStatus,
            @RequestParam(required = false) Instant fromDate,
            @RequestParam(required = false) Instant toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        var criteria = new TransactionSearchCriteria(
                minAmount,
                maxAmount,
                rrn,
                merchant,
                transactionStatus,
                fromDate,
                toDate
        );
        var pageable = PageRequest.of(page, size);
        return queries.searchAllForSupport(criteria, pageable)
                .map(PageResponse::from)
                .map(ApiResponse::ok);
    }
}
