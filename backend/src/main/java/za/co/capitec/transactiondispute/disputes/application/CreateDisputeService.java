package za.co.capitec.transactiondispute.disputes.application;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;
import za.co.capitec.transactiondispute.disputes.application.config.DisputesModuleProperties;
import za.co.capitec.transactiondispute.disputes.application.exceptions.*;
import za.co.capitec.transactiondispute.disputes.application.model.CreateDisputeCommand;
import za.co.capitec.transactiondispute.disputes.application.model.CreateDisputeResult;
import za.co.capitec.transactiondispute.disputes.application.model.DisputeHistoryItem;
import za.co.capitec.transactiondispute.disputes.application.port.in.CreateDisputeUseCase;
import za.co.capitec.transactiondispute.disputes.application.port.out.DisputeEventPublisherPort;
import za.co.capitec.transactiondispute.disputes.application.port.out.DisputeHistoryRepositoryPort;
import za.co.capitec.transactiondispute.disputes.application.port.out.DisputeRepositoryPort;
import za.co.capitec.transactiondispute.disputes.application.port.out.TransactionQueryPort;
import za.co.capitec.transactiondispute.disputes.domain.model.Dispute;
import za.co.capitec.transactiondispute.disputes.domain.model.TransactionStatus;
import za.co.capitec.transactiondispute.disputes.infrastructure.persistence.DisputeActor;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;

public class CreateDisputeService implements CreateDisputeUseCase {

    private static final String ACTOR_ROLE_USER = "USER";

    private final TransactionQueryPort transactions;
    private final DisputeRepositoryPort disputes;
    private final DisputeHistoryRepositoryPort history;
    private final DisputeEventPublisherPort events;
    private final TransactionalOperator disputesTxOperator;
    private final Clock clock;
    private final DisputesModuleProperties props;

    public CreateDisputeService(TransactionQueryPort transactions,
                                DisputeRepositoryPort disputes,
                                DisputeHistoryRepositoryPort history,
                                DisputeEventPublisherPort events,
                                TransactionalOperator disputesTxOperator,
                                Clock clock,
                                DisputesModuleProperties props
    ) {
        this.transactions = Objects.requireNonNull(transactions, "transactions");
        this.disputes = Objects.requireNonNull(disputes, "disputes");
        this.history = Objects.requireNonNull(history, "history");
        this.events = Objects.requireNonNull(events, "events");
        this.disputesTxOperator = Objects.requireNonNull(disputesTxOperator, "disputesTxOperator");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.props = Objects.requireNonNull(props, "props");
    }

    @Override
    @Transactional(transactionManager = "disputesTxManager", propagation = Propagation.NOT_SUPPORTED)// TX boundary is the operator-wrapped DB work. Event publish happens after commit.
    public Mono<CreateDisputeResult> create(CreateDisputeCommand command) {
        Objects.requireNonNull(command, "command");

        return createTransactional(command)
                .as(disputesTxOperator::transactional)
                // publish AFTER commit
                .flatMap(saved -> events.publishDisputeCreated(saved).thenReturn(saved))
                .map(d -> new CreateDisputeResult(
                        d.disputeId(),
                        d.transactionId(),
                        d.disputeStatus(),
                        d.createdAt()
                ));
    }

    private Mono<Dispute> createTransactional(CreateDisputeCommand command) {

        UUID userId = require(command.userId(), "userId");
        UUID transactionId = require(command.transactionId(), "transactionId");
        Instant now = Instant.now(clock);

        return transactions.findDisputableById(transactionId)
                .switchIfEmpty(Mono.error(new TransactionNotFoundException(transactionId)))
                .flatMap(tx -> {
                    // ownership
                    if (!userId.equals(tx.accountId())) {
                        return Mono.error(new TransactionNotOwnedException(transactionId, userId));
                    }

                    // eligibility
                    if (TransactionStatus.POSTED != tx.transactionStatus()) {
                        return Mono.error(new TransactionNotDisputableException(transactionId, tx.transactionStatus().name()));
                    }

                    // dispute window
                    int maxAgeDays = props.rules().maxAgeDays();
                    Instant oldestAllowed = now.minus(maxAgeDays, ChronoUnit.DAYS);
                    if (tx.postedAt() != null && tx.postedAt().isBefore(oldestAllowed)) {
                        return Mono.error(new TransactionDisputeWindowExpiredException(
                                transactionId,
                                tx.postedAt(),
                                maxAgeDays
                        ));
                    }

                    // duplicate rule aligned with DB: (transaction_id, user_id)
                    return disputes.existsForTransactionAndUser(transactionId, userId)
                            .flatMap(exists -> {
                                if (Boolean.TRUE.equals(exists)) {
                                    return Mono.error(new OpenDisputeAlreadyExistsException(transactionId));
                                }

                                UUID disputeId = UUID.randomUUID();
                                Dispute dispute = Dispute.openDispute(
                                        disputeId,
                                        transactionId,
                                        userId,
                                        require(command.reasonCode(), "reasonCode"),
                                        require(command.note(), "note"),
                                        now
                                );

                                DisputeHistoryItem historyItem = new DisputeHistoryItem(
                                        UUID.randomUUID(),
                                        disputeId,
                                        null,
                                        dispute.disputeStatus(),
                                        dispute.note(),
                                        new DisputeActor(ACTOR_ROLE_USER, userId),
                                        now
                                );

                                return disputes.save(dispute)
                                        // Race-safe: if DB unique constraint trips, map it to same 409
                                        .onErrorMap(DuplicateKeyException.class,
                                                ex -> new OpenDisputeAlreadyExistsException(transactionId))
                                        .flatMap(saved -> history.append(historyItem).thenReturn(saved));
                            });
                });
    }

    private static <T> T require(T value, String name) {
        return Objects.requireNonNull(value, name);
    }
}
