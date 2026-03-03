package za.co.capitec.transactiondispute.disputes.application.port.out;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import za.co.capitec.transactiondispute.disputes.domain.model.Dispute;
import za.co.capitec.transactiondispute.disputes.domain.model.DisputeStatus;

import java.util.UUID;

public interface DisputeRepositoryPort {
    Mono<Dispute> save(Dispute dispute);
    Mono<Boolean> existsForTransactionAndUser(UUID transactionId, UUID userId);
    Flux<Dispute> findByUserId(UUID userId);
    Mono<Page<Dispute>> findByDisputeStatus(DisputeStatus disputeStatus, Pageable pageable);
    Mono<Dispute> findById(UUID disputeId);
}
