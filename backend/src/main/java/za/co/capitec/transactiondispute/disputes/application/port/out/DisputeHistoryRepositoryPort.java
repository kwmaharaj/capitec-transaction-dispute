package za.co.capitec.transactiondispute.disputes.application.port.out;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import za.co.capitec.transactiondispute.disputes.application.model.DisputeHistoryItem;

import java.util.UUID;

public interface DisputeHistoryRepositoryPort {

    Mono<Void> append(DisputeHistoryItem item);

    Flux<DisputeHistoryItem> findByDisputeId(UUID disputeId);
}
