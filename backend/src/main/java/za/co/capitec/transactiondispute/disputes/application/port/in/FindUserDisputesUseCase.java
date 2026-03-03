package za.co.capitec.transactiondispute.disputes.application.port.in;

import reactor.core.publisher.Flux;
import za.co.capitec.transactiondispute.disputes.domain.model.Dispute;

import java.util.UUID;

public interface FindUserDisputesUseCase {

    Flux<Dispute> findByUserId(UUID userId);
}
