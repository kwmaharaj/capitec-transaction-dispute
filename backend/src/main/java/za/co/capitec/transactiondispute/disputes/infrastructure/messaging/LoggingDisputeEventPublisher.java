package za.co.capitec.transactiondispute.disputes.infrastructure.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import za.co.capitec.transactiondispute.disputes.application.port.out.DisputeEventPublisherPort;
import za.co.capitec.transactiondispute.disputes.domain.event.DisputeStatusChanged;
import za.co.capitec.transactiondispute.disputes.domain.model.Dispute;

/**
 * placeholder publisher (log only).
 * can later be swapped for Kafka/outbox etc.
 */
public class LoggingDisputeEventPublisher implements DisputeEventPublisherPort {

    private static final Logger LOG = LoggerFactory.getLogger(LoggingDisputeEventPublisher.class);

    @Override
    public Mono<Void> publishDisputeCreated(Dispute dispute) {
        LOG.info("dispute.created disputeId={} transactionId={} userId={} reasonCode={} status={}",
                dispute.disputeId(),
                dispute.transactionId(),
                dispute.userId(),
                dispute.reasonCode(),
                dispute.disputeStatus());
        return Mono.empty();
    }

    @Override
    public Mono<Void> publishDisputeStatusChanged(DisputeStatusChanged event) {
        LOG.info(
                "dispute.status-changed disputeId={} transactionId={} userId={} decidedBy={} previousStatus={} newStatus={} occurredAt={}",
                event.disputeId(),
                event.transactionId(),
                event.userId(),
                event.decidedBy(),
                event.previousStatus(),
                event.newStatus(),
                event.occurredAt()
        );
        return Mono.empty();
    }
}
