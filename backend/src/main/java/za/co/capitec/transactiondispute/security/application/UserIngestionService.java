package za.co.capitec.transactiondispute.security.application;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import za.co.capitec.transactiondispute.security.application.port.in.UserIngestionUseCase;
import za.co.capitec.transactiondispute.security.application.port.out.UserRepositoryPort;
import za.co.capitec.transactiondispute.security.domain.model.UserAccount;

public class UserIngestionService implements UserIngestionUseCase {

    private final UserRepositoryPort repo;

    public UserIngestionService(UserRepositoryPort repo) {
        this.repo = repo;
    }

    @Override
    public Mono<Void> ingest(Flux<UserAccount> users) {
        return repo.upsertAll(users);
    }
}
