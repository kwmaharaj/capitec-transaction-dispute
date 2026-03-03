package za.co.capitec.transactiondispute.shared.infrastructure.http;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.boot.test.web.server.LocalServerPort;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;
import za.co.capitec.transactiondispute.testsupport.IntegrationTestBase;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class IdempotencyWebFilterIT extends IntegrationTestBase {

    @LocalServerPort
    int port;

    @Test
    void postWithoutIdempotencyKey_returns400() {
        UUID userId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        ingestUser(userId, "idem-user", "idem-pass", java.util.List.of("ROLE_USER"));
        String token = loginAndGetToken("idem-user", "idem-pass");

        webClient.post()
                .uri("/v1/test/idempotency/fast")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("a", 1))
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void duplicateAfterCompletion_replaysSameResponse_andDoesNotReExecuteController() throws IOException {
        UUID userId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        ingestUser(userId, "idem-user2", "idem-pass", java.util.List.of("ROLE_USER"));
        String token = loginAndGetToken("idem-user2", "idem-pass");

        String idemKey = "abc-123";
        var om = new com.fasterxml.jackson.databind.ObjectMapper();

        // baseline count BEFORE we call the endpoint
        byte[] baselineBytes = webClient.get()
                .uri("/v1/test/idempotency/executions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .returnResult()
                .getResponseBodyContent();

        assertThat(baselineBytes).isNotNull();
        int baseline = om.readTree(baselineBytes).get("count").asInt();

        byte[] first = webClient.post()
                .uri("/v1/test/idempotency/fast")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .header(IdempotencyWebFilter.HEADER_IDEMPOTENCY_KEY, idemKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("x", "y"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .returnResult()
                .getResponseBodyContent();

        byte[] second = webClient.post()
                .uri("/v1/test/idempotency/fast")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .header(IdempotencyWebFilter.HEADER_IDEMPOTENCY_KEY, idemKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("x", "y"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .returnResult()
                .getResponseBodyContent();

        assertThat(first).isNotNull();
        assertThat(second).isNotNull();

        var firstJson = om.readTree(first);
        var secondJson = om.readTree(second);

        assertThat(secondJson.get("resultId").asText()).isEqualTo(firstJson.get("resultId").asText());
        assertThat(secondJson.get("echo")).hasToString(firstJson.get("echo").toString());

        // Replay means controller wasn't executed again
        assertThat(secondJson.get("execution").asInt()).isEqualTo(firstJson.get("execution").asInt());

        webClient.get()
                .uri("/v1/test/idempotency/executions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.count").isEqualTo(baseline + 1);
    }

    @Test
    void duplicateWhileInProgress_returns409_andOriginalCompletes() {
        UUID userId = UUID.fromString("33333333-3333-3333-3333-333333333333");
        ingestUser(userId, "idem-user3", "idem-pass", java.util.List.of("ROLE_USER"));
        String token = loginAndGetToken("idem-user3", "idem-pass");

        WebClient wc = WebClient.builder()
                .baseUrl("http://localhost:" + port)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .build();

        String idemKey = "slow-1";

        Mono<String> first = wc.post()
                .uri("/v1/test/idempotency/slow")
                .header(IdempotencyWebFilter.HEADER_IDEMPOTENCY_KEY, idemKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("k", "v"))
                .retrieve()
                .bodyToMono(String.class)
                .subscribeOn(Schedulers.parallel());

        Mono<Integer> secondStatus = wc.post()
                .uri("/v1/test/idempotency/slow")
                .header(IdempotencyWebFilter.HEADER_IDEMPOTENCY_KEY, idemKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("k", "v"))
                .exchangeToMono(resp -> Mono.just(resp.statusCode().value()))
                .delaySubscription(Duration.ofMillis(40));

        StepVerifier.create(Mono.zip(first, secondStatus))
                .assertNext(tuple -> {
                    String body = tuple.getT1();
                    int status = tuple.getT2();
                    assertThat(body).contains("resultId");
                    assertThat(status).isEqualTo(409);
                })
                .verifyComplete();
    }
}
