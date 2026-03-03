package za.co.capitec.transactiondispute.disputes.application.port.out;

import reactor.core.publisher.Mono;

import za.co.capitec.transactiondispute.disputes.domain.event.DisputeStatusChanged;
import za.co.capitec.transactiondispute.disputes.domain.model.Dispute;

/**
 * implementation can be changed to kafka later
 */
public interface DisputeEventPublisherPort {
    Mono<Void> publishDisputeCreated(Dispute dispute);

    //Raised when a dispute status changes (OPEN -> IN_PROGRESS, IN_PROGRESS -> RESOLVED/REJECTED).
    Mono<Void> publishDisputeStatusChanged(DisputeStatusChanged event);
}