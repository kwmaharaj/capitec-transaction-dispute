package za.co.capitec.transactiondispute.disputes.application.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Mono;
import za.co.capitec.transactiondispute.disputes.application.port.in.FindDisputesUseCase;
import za.co.capitec.transactiondispute.disputes.application.port.out.DisputeRepositoryPort;
import za.co.capitec.transactiondispute.disputes.domain.model.Dispute;
import za.co.capitec.transactiondispute.disputes.domain.model.DisputeStatus;

public class FindDisputesService implements FindDisputesUseCase {

    private final DisputeRepositoryPort repo;

    public FindDisputesService(DisputeRepositoryPort repo) {
        this.repo = repo;
    }

    @Override
    public Mono<Page<Dispute>> findByDisputeStatus(DisputeStatus disputeStatus, Pageable pageable) {
        return repo.findByDisputeStatus(disputeStatus, pageable);
    }
}