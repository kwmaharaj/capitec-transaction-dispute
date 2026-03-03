package za.co.capitec.transactiondispute.security.interfaces.http;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import za.co.capitec.transactiondispute.security.application.PasswordService;
import za.co.capitec.transactiondispute.security.application.port.in.UserIngestionUseCase;
import za.co.capitec.transactiondispute.security.domain.model.UserAccount;

import java.util.List;
import java.util.UUID;

/**
 * INTERNAL: Seed demo/test users via HTTP.
 *
 * Protected by ROLE_SUPPORT via SecurityConfig.
 */
@RestController
@RequestMapping("/internal/ingest")
public class UserIngestionController {

    private final UserIngestionUseCase ingestion;
    private final PasswordService passwords;

    public UserIngestionController(UserIngestionUseCase ingestion, PasswordService passwords) {
        this.ingestion = ingestion;
        this.passwords = passwords;
    }

    @PostMapping("/users")
    public Mono<Void> ingestUsers(@RequestBody Flux<IngestUserRequest> body) {
        var users = body.flatMap(req -> passwords.hash(req.password())
                .map(req::toDomain));

        return ingestion.ingest(users);
    }

    public record IngestUserRequest(UUID userId, String username, String password, List<String> roles) {
        UserAccount toDomain(String passwordHash) {
            return new UserAccount(userId, username, passwordHash, roles);
        }
    }
}
