package za.co.capitec.transactiondispute.disputes.application.port.in;

import reactor.core.publisher.Flux;
import za.co.capitec.transactiondispute.disputes.application.model.DisputeHistoryItem;

import java.util.UUID;

public interface DisputeHistoryUseCase {
    Flux<DisputeHistoryItem> findForUser(UUID disputeId, UUID userId);
    Flux<DisputeHistoryItem> findForSupport(UUID disputeId);
}
