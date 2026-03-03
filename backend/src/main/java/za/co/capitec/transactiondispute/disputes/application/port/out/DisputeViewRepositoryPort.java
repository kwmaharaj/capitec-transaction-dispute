package za.co.capitec.transactiondispute.disputes.application.port.out;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Mono;
import za.co.capitec.transactiondispute.disputes.application.model.DisputeSearchCriteria;
import za.co.capitec.transactiondispute.disputes.application.model.DisputeTransactionViewRow;

import java.util.UUID;

public interface DisputeViewRepositoryPort {

    Mono<Page<DisputeTransactionViewRow>> searchForUser(UUID userId, DisputeSearchCriteria criteria, Pageable pageable);

    Mono<Page<DisputeTransactionViewRow>> searchForSupport(DisputeSearchCriteria criteria, Pageable pageable);
}
