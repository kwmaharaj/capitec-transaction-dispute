package za.co.capitec.transactiondispute.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import za.co.capitec.transactiondispute.security.application.AuthService;
import za.co.capitec.transactiondispute.security.application.JwtTokenService;
import za.co.capitec.transactiondispute.security.application.PasswordService;
import za.co.capitec.transactiondispute.security.application.TokenRevocationService;
import za.co.capitec.transactiondispute.security.application.port.out.UserRepositoryPort;
import za.co.capitec.transactiondispute.security.domain.model.UserAccount;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AuthServiceTest {

    private UserRepositoryPort users;
    private PasswordService passwords;
    private JwtTokenService tokens;
    private TokenRevocationService revocation;

    private AuthService service;

    @BeforeEach
    void setUp() {
        users = mock(UserRepositoryPort.class);
        passwords = mock(PasswordService.class);
        tokens = mock(JwtTokenService.class);
        revocation = mock(TokenRevocationService.class);

        service = new AuthService(users, passwords, tokens, revocation);
    }

    @Test
    void login_whenUserNotFound_returnsUnauthorized() {
        when(users.findByUsername("bob")).thenReturn(Mono.empty());

        StepVerifier.create(service.login("bob", "pw"))
                .expectErrorSatisfies(ex -> {
                    org.assertj.core.api.Assertions.assertThat(ex).isInstanceOf(ResponseStatusException.class);
                    var rse = (ResponseStatusException) ex;
                    org.assertj.core.api.Assertions.assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
                })
                .verify();
    }

    @Test
    void login_whenPasswordMismatch_returnsUnauthorized() {
        var user = new UserAccount(UUID.randomUUID(), "bob", "passwordHash", List.of("USER"));

        when(users.findByUsername("bob")).thenReturn(Mono.just(user));
        when(passwords.matches("wrong", "passwordHash")).thenReturn(Mono.just(false));

        StepVerifier.create(service.login("bob", "wrong"))
                .expectErrorSatisfies(ex -> {
                    org.assertj.core.api.Assertions.assertThat(ex).isInstanceOf(ResponseStatusException.class);
                    var rse = (ResponseStatusException) ex;
                    org.assertj.core.api.Assertions.assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
                })
                .verify();

        verify(tokens, never()).issueAccessToken(any(), any());
    }

    @Test
    void login_success_issuesToken_andReturnsLoginResult() {
        var userId = UUID.randomUUID();
        var user = new UserAccount(userId, "bob", "passwordHash", List.of("USER", "SUPPORT"));
        when(users.findByUsername("bob")).thenReturn(Mono.just(user));
        when(passwords.matches("pw", "passwordHash")).thenReturn(Mono.just(true));
        when(tokens.issueAccessToken(userId, user.roles())).thenReturn("jwt-x");

        StepVerifier.create(service.login("bob", "pw"))
                .assertNext(res -> {
                    org.assertj.core.api.Assertions.assertThat(res.accessToken()).isEqualTo("jwt-x");
                    org.assertj.core.api.Assertions.assertThat(res.userId()).isEqualTo(userId);
                    org.assertj.core.api.Assertions.assertThat(res.roles()).containsExactly("USER", "SUPPORT");
                })
                .verifyComplete();
    }

    @Test
    void register_whenUsernameAlreadyExists_returnsConflict() {
        var existing = new UserAccount(UUID.randomUUID(), "bob", "passwordHash", List.of("USER"));
        when(passwords.hash(anyString())).thenReturn(Mono.just("passwordHash"));
        when(users.findByUsername(anyString())).thenReturn(Mono.just(existing));
        StepVerifier.create(service.register("bob", "pw"))
                .expectErrorSatisfies(ex -> {
                    var rse = (ResponseStatusException) ex;
                    org.assertj.core.api.Assertions.assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                })
                .verify();
        verify(users, never()).insert(any());
    }

    @Test
    void register_success_hashesPassword_insertsUser_andReturnsRegisterResult() {
        when(users.findByUsername("alice")).thenReturn(Mono.empty());
        when(passwords.hash("pw")).thenReturn(Mono.just("hash-x"));
        when(users.insert(any())).thenReturn(Mono.empty());

        StepVerifier.create(service.register("alice", "pw"))
                .assertNext(res -> {
                    org.assertj.core.api.Assertions.assertThat(res.userId()).isNotNull();
                    org.assertj.core.api.Assertions.assertThat(res.username()).isEqualTo("alice");
                    org.assertj.core.api.Assertions.assertThat(res.roles()).containsExactly("USER");
                })
                .verifyComplete();

        var captor = ArgumentCaptor.forClass(UserAccount.class);
        verify(users).insert(captor.capture());
        org.assertj.core.api.Assertions.assertThat(captor.getValue().username()).isEqualTo("alice");
        org.assertj.core.api.Assertions.assertThat(captor.getValue().passwordHash()).isEqualTo("hash-x");
        org.assertj.core.api.Assertions.assertThat(captor.getValue().roles()).containsExactly("USER");
    }

    @Test
    void logout_withBlankInputs_returnsEmpty_andDoesNotRevoke() {
        StepVerifier.create(service.logout(" ", Instant.now().plusSeconds(60)))
                .verifyComplete();

        StepVerifier.create(service.logout("jti", null))
                .verifyComplete();

        verifyNoInteractions(revocation);
    }

    @Test
    void logout_withValidInputs_revokesWithPositiveTtl() {
        when(revocation.revoke(anyString(), any())).thenReturn(Mono.empty());

        var expiresAt = Instant.now().plusSeconds(120);
        StepVerifier.create(service.logout("jti-1", expiresAt))
                .verifyComplete();

        var ttlCaptor = ArgumentCaptor.forClass(Duration.class);
        verify(revocation).revoke(eq("jti-1"), ttlCaptor.capture());
        org.assertj.core.api.Assertions.assertThat(ttlCaptor.getValue().toMillis()).isPositive();
    }
}
