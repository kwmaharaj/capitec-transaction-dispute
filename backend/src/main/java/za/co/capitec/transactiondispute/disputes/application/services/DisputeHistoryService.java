package za.co.capitec.transactiondispute.disputes.application.services;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import za.co.capitec.transactiondispute.disputes.application.exceptions.DisputeNotFoundException;
import za.co.capitec.transactiondispute.disputes.application.model.DisputeHistoryItem;
import za.co.capitec.transactiondispute.disputes.application.port.in.DisputeHistoryUseCase;
import za.co.capitec.transactiondispute.disputes.application.port.out.DisputeHistoryRepositoryPort;
import za.co.capitec.transactiondispute.disputes.application.port.out.DisputeRepositoryPort;

import java.util.Objects;
import java.util.UUID;

public class DisputeHistoryService implements DisputeHistoryUseCase {

    private final DisputeRepositoryPort disputes;
    private final DisputeHistoryRepositoryPort history;

    public DisputeHistoryService(DisputeRepositoryPort disputes, DisputeHistoryRepositoryPort history) {
        this.disputes = Objects.requireNonNull(disputes, "disputes");
        this.history = Objects.requireNonNull(history, "history");
    }

    @Override
    public Flux<DisputeHistoryItem> findForUser(UUID disputeId, UUID userId) {
        return disputes.findById(disputeId)
                .switchIfEmpty(Mono.error(new DisputeNotFoundException(disputeId)))
                // hide existence if not owned
                .filter(d -> userId.equals(d.userId()))
                .switchIfEmpty(Mono.error(new DisputeNotFoundException(disputeId)))
                .flatMapMany(d -> history.findByDisputeId(disputeId));
    }

    @Override
    public Flux<DisputeHistoryItem> findForSupport(UUID disputeId) {
        return disputes.findById(disputeId)
                .switchIfEmpty(Mono.error(new DisputeNotFoundException(disputeId)))
                .flatMapMany(d -> history.findByDisputeId(disputeId));
    }
}
