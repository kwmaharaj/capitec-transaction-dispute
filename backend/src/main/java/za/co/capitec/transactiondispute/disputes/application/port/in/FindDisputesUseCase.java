package za.co.capitec.transactiondispute.disputes.application.port.in;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Mono;
import za.co.capitec.transactiondispute.disputes.domain.model.Dispute;
import za.co.capitec.transactiondispute.disputes.domain.model.DisputeStatus;

public interface FindDisputesUseCase {

    Mono<Page<Dispute>> findByDisputeStatus(DisputeStatus disputeStatus, Pageable pageable);
}
