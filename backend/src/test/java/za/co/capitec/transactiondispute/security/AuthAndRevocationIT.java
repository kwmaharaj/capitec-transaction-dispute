package za.co.capitec.transactiondispute.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import za.co.capitec.transactiondispute.security.application.TokenRevocationService;
import za.co.capitec.transactiondispute.testsupport.IntegrationTestBase;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

class AuthAndRevocationIT extends IntegrationTestBase {

    @Autowired
    TokenRevocationService revocations;

    @Autowired
    ReactiveJwtDecoder jwtDecoder;

    @Test
    void login_then_logout_revokes_token_and_subsequent_calls_are_unauthorized() {

        String username = "alice-" + UUID.randomUUID().toString().substring(0, 8);
        String password = "user1-pass";

        // REGISTER (capture body even on failure)
        EntityExchangeResult<String> registerResult = webClient.post()
                .uri("/auth/register")
                .bodyValue(Map.of("username", username, "password", password))
                .exchange()
                .expectBody(String.class)
                .returnResult();

        if (registerResult.getStatus() != HttpStatus.OK) {
            fail("Register failed. status=%s body=%s"
                    .formatted(registerResult.getStatus(), registerResult.getResponseBody()));
        }

        // LOGIN
        String token = loginAndGetToken(username, password);

        // Decode token and ensure we actually have a JTI (otherwise logout can't revoke)
        var jwt = jwtDecoder.decode(token).block();
        assertThat(jwt).isNotNull();

        String jti = jwt.getId();
        assertThat(jti)
                .as("Access token must contain jti so logout can revoke it")
                .isNotBlank();

        // sanity: token works on protected endpoint
        webClient.get()
                .uri("/v1/transactions")
                .headers(h -> h.setBearerAuth(token))
                .exchange()
                .expectStatus().is2xxSuccessful();

        // logout revokes JTI in Redis
        webClient.post()
                .uri("/auth/logout")
                .headers(h -> h.setBearerAuth(token))
                .exchange()
                .expectStatus().is2xxSuccessful();

        // Verify revocation exists (if this fails, logout isn't revoking anything)
        Boolean revoked = revocations.isRevoked(jti).block();
        assertThat(revoked)
                .as("Expected Redis to contain revocation entry for jti=%s".formatted(jti))
                .isTrue();

        // same token must now be rejected during authentication
        EntityExchangeResult<String> afterLogout = webClient.get()
                .uri("/v1/transactions")
                .headers(h -> h.setBearerAuth(token))
                .exchange()
                .expectBody(String.class)
                .returnResult();

        assertThat(afterLogout.getStatus())
                .as("Expected revoked token to be rejected. body=%s".formatted(afterLogout.getResponseBody()))
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}