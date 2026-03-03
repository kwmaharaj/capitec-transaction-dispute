package za.co.capitec.transactiondispute.security.application.port.in;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import za.co.capitec.transactiondispute.security.domain.model.UserAccount;

/**
 * Internal ingestion for seeding demo/test users.
 */
public interface UserIngestionUseCase {
    Mono<Void> ingest(Flux<UserAccount> users);
}
