package za.co.capitec.transactiondispute.security.application;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import za.co.capitec.transactiondispute.security.application.port.out.UserRepositoryPort;
import za.co.capitec.transactiondispute.security.domain.model.UserAccount;

import java.util.List;
import java.util.UUID;

public class AuthService {

    private final UserRepositoryPort users;
    private final PasswordService passwords;
    private final JwtTokenService tokens;
    private final TokenRevocationService revocationService;

    public AuthService(UserRepositoryPort users, PasswordService passwords, JwtTokenService tokens, TokenRevocationService revocationService) {
        this.users = users;
        this.passwords = passwords;
        this.tokens = tokens;
        this.revocationService=revocationService;
    }

    public Mono<LoginResult> login(String username, String password) {
        return users.findByUsername(username)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials")))
                .flatMap(user -> passwords.matches(password, user.passwordHash())
                        .flatMap(match -> Boolean.TRUE.equals(match)
                                        ? Mono.fromSupplier(() -> {
                                    var jwt = tokens.issueAccessToken(user.userId(), user.roles());
                                    return new LoginResult(jwt, user.userId(), user.roles());
                                })
                                        : Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"))
                        ));
    }

    public Mono<RegisterResult> register(String username, String password) {
        return users.findByUsername(username)
                .flatMap(existing -> Mono.<RegisterResult>error(new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists")))
                .switchIfEmpty(
                        passwords.hash(password)
                                .flatMap(hash -> {
                                    var userId = UUID.randomUUID();
                                    var roles = List.of("USER");
                                    var account = new UserAccount(userId, username, hash, roles);
                                    return users.insert(account)
                                            .thenReturn(new RegisterResult(userId, username, roles));
                                })
                );
    }

    public Mono<Void> logout(String jti, java.time.Instant expiresAt) {
        if (jti == null || jti.isBlank() || expiresAt == null) {
            return Mono.empty();
        }
        var ttl = java.time.Duration.between(java.time.Instant.now(), expiresAt);
        return revocationService.revoke(jti, ttl);
    }

    public record LoginResult(String accessToken, UUID userId, List<String> roles) {}

    public record RegisterResult(UUID userId, String username, List<String> roles) {}
}
