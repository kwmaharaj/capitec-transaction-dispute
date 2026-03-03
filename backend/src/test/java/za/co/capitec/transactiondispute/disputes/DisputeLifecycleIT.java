package za.co.capitec.transactiondispute.disputes;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import za.co.capitec.transactiondispute.shared.infrastructure.http.IdempotencyWebFilter;
import za.co.capitec.transactiondispute.testsupport.IntegrationTestBase;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

class DisputeLifecycleIT extends IntegrationTestBase {

    @Test
    void user_creates_dispute_support_processes_it_and_history_is_recorded() {
        UUID user = UUID.randomUUID();
        UUID support = UUID.randomUUID();
        UUID tx = UUID.randomUUID();

        ingestUser(user, "bob", "pw", List.of("ROLE_USER"));
        ingestUser(support, "agent", "pw", List.of("ROLE_SUPPORT"));

        ingestTransaction(Map.ofEntries(
                Map.entry("transactionId", tx.toString()),
                Map.entry("userId", user.toString()),
                Map.entry("postedAt", Instant.now().minusSeconds(3600).toString()),
                Map.entry("amount", new BigDecimal("12.34")),
                Map.entry("currency", "ZAR"),
                Map.entry("merchant", "SHOP"),
                Map.entry("status", "POSTED"),
                Map.entry("bin", "411111"),
                Map.entry("last4digits", "1111"),
                Map.entry("panHash", "hash"),
                Map.entry("rrn", "rrn-xyz")
        ));

        String userToken = loginAndGetToken("bob", "pw");
        String supportToken = loginAndGetToken("agent", "pw");

        byte[] createBody = webClient.post()
                .uri("/v1/disputes")
                .headers(h -> {
                    h.setBearerAuth(userToken);
                    h.set(IdempotencyWebFilter.HEADER_IDEMPOTENCY_KEY, UUID.randomUUID().toString());
                })
                .bodyValue(Map.of(
                        "transactionId", tx.toString(),
                        "reasonCode", "FRAUD_SUSPECTED",
                        "note", "suspicious"
                ))
                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectBody()
                .returnResult()
                .getResponseBodyContent();

        if (createBody == null) {
            throw new IllegalStateException("Create dispute response was empty");
        }
        String disputeId = JsonPath.read(new String(createBody), "$.data.disputeId");

        // user can see it in their list
        webClient.get()
                .uri("/v1/disputes")
                .headers(h -> {
                    h.setBearerAuth(userToken);
                    h.set(IdempotencyWebFilter.HEADER_IDEMPOTENCY_KEY, UUID.randomUUID().toString());
                })
                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectBody()
                .jsonPath("$.data[0].disputeId").isEqualTo(disputeId);

        // support moves it to IN_PROGRESS
        webClient.post()
                .uri("/v1/support/disputes/" + disputeId + "/decision")
                .headers(h -> {
                    h.setBearerAuth(supportToken);
                    h.set(IdempotencyWebFilter.HEADER_IDEMPOTENCY_KEY, UUID.randomUUID().toString());
                })
                .bodyValue(Map.of("disputeStatus", "IN_PROGRESS", "supportNote", "investigating"))
                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectBody()
                .jsonPath("$.data.disputeStatus").isEqualTo("IN_PROGRESS");

        // support resolves it
        webClient.post()
                .uri("/v1/support/disputes/" + disputeId + "/decision")
                .headers(h -> {
                    h.setBearerAuth(supportToken);
                    h.set(IdempotencyWebFilter.HEADER_IDEMPOTENCY_KEY, UUID.randomUUID().toString());
                })
                .bodyValue(Map.of("disputeStatus", "RESOLVED", "supportNote", "ok"))
                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectBody()
                .jsonPath("$.data.disputeStatus").isEqualTo("RESOLVED");

        // user can fetch history
        webClient.get()
                .uri("/v1/disputes/" + disputeId + "/history")
                .headers(h -> h.setBearerAuth(userToken))
                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectBody()
                .jsonPath("$.data.length()").isNumber();

        // support can see history too
        webClient.get()
                .uri("/v1/support/disputes/" + disputeId + "/history")
                .headers(h -> h.setBearerAuth(supportToken))
                .exchange()
                .expectStatus().is2xxSuccessful();
    }
}
