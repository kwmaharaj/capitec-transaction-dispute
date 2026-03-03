package za.co.capitec.transactiondispute.disputes.application.services;

import reactor.core.publisher.Flux;
import za.co.capitec.transactiondispute.disputes.application.port.in.FindUserDisputesUseCase;
import za.co.capitec.transactiondispute.disputes.application.port.out.DisputeRepositoryPort;
import za.co.capitec.transactiondispute.disputes.domain.model.Dispute;

import java.util.UUID;

public class FindUserDisputesService implements FindUserDisputesUseCase {

    private final DisputeRepositoryPort repo;

    public FindUserDisputesService(DisputeRepositoryPort repo) {
        this.repo = repo;
    }

    @Override
    public Flux<Dispute> findByUserId(UUID userId) {
        return repo.findByUserId(userId);
    }
}