package za.co.capitec.transactiondispute.security.application.port.out;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import za.co.capitec.transactiondispute.security.domain.model.UserAccount;

public interface UserRepositoryPort {

    Mono<UserAccount> findByUsername(String username);

    Mono<Void> insert(UserAccount user);

    Mono<Void> upsertAll(Flux<UserAccount> users);
}
