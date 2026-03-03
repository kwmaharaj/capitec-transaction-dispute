package za.co.capitec.transactiondispute.disputes.application.port.in;

import reactor.core.publisher.Mono;
import za.co.capitec.transactiondispute.disputes.domain.model.Dispute;
import za.co.capitec.transactiondispute.disputes.domain.model.DisputeStatus;

import java.util.UUID;

public interface DecideDisputeUseCase {
    Mono<Dispute> decide(UUID disputeId, UUID supportUserId, DisputeStatus status, String supportNote);
}