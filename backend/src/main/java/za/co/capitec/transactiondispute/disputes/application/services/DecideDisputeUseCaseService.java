package za.co.capitec.transactiondispute.disputes.application.services;

import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;
import za.co.capitec.transactiondispute.disputes.application.exceptions.DisputeNotFoundException;
import za.co.capitec.transactiondispute.disputes.application.model.DisputeHistoryItem;
import za.co.capitec.transactiondispute.disputes.application.port.in.DecideDisputeUseCase;
import za.co.capitec.transactiondispute.disputes.application.port.out.DisputeEventPublisherPort;
import za.co.capitec.transactiondispute.disputes.application.port.out.DisputeHistoryRepositoryPort;
import za.co.capitec.transactiondispute.disputes.application.port.out.DisputeRepositoryPort;
import za.co.capitec.transactiondispute.disputes.domain.event.DisputeStatusChanged;
import za.co.capitec.transactiondispute.disputes.domain.model.Dispute;
import za.co.capitec.transactiondispute.disputes.domain.model.DisputeStatus;
import za.co.capitec.transactiondispute.disputes.infrastructure.persistence.DisputeActor;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class DecideDisputeUseCaseService implements DecideDisputeUseCase {

    private static final String ACTOR_ROLE_SUPPORT = "SUPPORT";

    private final DisputeRepositoryPort disputes;
    private final DisputeHistoryRepositoryPort history;
    private final DisputeEventPublisherPort events;
    private final TransactionalOperator disputesTxOperator;
    private final Clock clock;

    public DecideDisputeUseCaseService(DisputeRepositoryPort disputes,
                                       DisputeHistoryRepositoryPort history,
                                       DisputeEventPublisherPort events,
                                       TransactionalOperator disputesTxOperator,
                                       Clock clock) {
        this.disputes = Objects.requireNonNull(disputes, "disputes");
        this.history = Objects.requireNonNull(history, "history");
        this.events = Objects.requireNonNull(events, "events");
        this.disputesTxOperator = Objects.requireNonNull(disputesTxOperator, "disputesTxOperator");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    // DB changes are transactional via operator. Events publish after commit.
    @Transactional(transactionManager = "disputesTxManager", propagation = Propagation.NOT_SUPPORTED)
    public Mono<Dispute> decide(UUID disputeId, UUID supportUserId, DisputeStatus disputeStatus, String supportNote) {

        return decideTransactional(disputeId, supportUserId, disputeStatus, supportNote)
                .as(disputesTxOperator::transactional)
                .flatMap(result -> {
                    if (result.event == null) {
                        return Mono.just(result.saved);
                    }
                    return events.publishDisputeStatusChanged(result.event).thenReturn(result.saved);
                });
    }

    private Mono<DecisionResult> decideTransactional(UUID disputeId, UUID supportUserId, DisputeStatus disputeStatus, String supportNote) {
        Instant now = Instant.now(clock);

        return disputes.findById(disputeId)
                .switchIfEmpty(Mono.error(new DisputeNotFoundException(disputeId)))
                .flatMap(existing -> {
                    var previous = existing.disputeStatus();
                    var updated = existing.decide(disputeStatus, Objects.requireNonNull(supportNote, "supportNote"));

                    if (previous == updated.disputeStatus()) {
                        // no change
                        return disputes.save(updated).map(saved -> new DecisionResult(saved, null));
                    }

                    DisputeHistoryItem historyItem = new DisputeHistoryItem(
                            UUID.randomUUID(),
                            updated.disputeId(),
                            previous,
                            updated.disputeStatus(),
                            updated.note(),
                            new DisputeActor(ACTOR_ROLE_SUPPORT,supportUserId),
                            now
                    );

                    return disputes.save(updated)
                            .flatMap(saved -> history.append(historyItem).thenReturn(saved))
                            .map(saved -> {
                                var ev = new DisputeStatusChanged(
                                        saved.disputeId(),
                                        saved.transactionId(),
                                        saved.userId(),
                                        supportUserId,
                                        previous,
                                        saved.disputeStatus(),
                                        saved.note(),
                                        saved.updatedAt() != null ? saved.updatedAt() : now
                                );
                                return new DecisionResult(saved, ev);
                            });
                });
    }

    private record DecisionResult(Dispute saved, DisputeStatusChanged event) {}
}
