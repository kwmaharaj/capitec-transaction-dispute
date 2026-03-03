package za.co.capitec.transactiondispute.disputes.application.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Mono;
import za.co.capitec.transactiondispute.disputes.application.model.DisputeSearchCriteria;
import za.co.capitec.transactiondispute.disputes.application.model.DisputeTransactionViewRow;
import za.co.capitec.transactiondispute.disputes.application.port.in.DisputeViewQueryUseCase;
import za.co.capitec.transactiondispute.disputes.application.port.out.DisputeViewRepositoryPort;

import java.util.Objects;
import java.util.UUID;

public class DisputeViewQueryService implements DisputeViewQueryUseCase {

    private final DisputeViewRepositoryPort repo;

    public DisputeViewQueryService(DisputeViewRepositoryPort repo) {
        this.repo = Objects.requireNonNull(repo, "repo");
    }

    @Override
    public Mono<Page<DisputeTransactionViewRow>> searchForUser(UUID userId, DisputeSearchCriteria criteria, Pageable pageable) {
        return repo.searchForUser(userId, criteria, pageable);
    }

    @Override
    public Mono<Page<DisputeTransactionViewRow>> searchForSupport(DisputeSearchCriteria criteria, Pageable pageable) {
        return repo.searchForSupport(criteria, pageable);
    }
}
