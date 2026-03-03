package za.co.capitec.transactiondispute.transactions;

import org.junit.jupiter.api.Test;
import za.co.capitec.transactiondispute.testsupport.IntegrationTestBase;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

class TransactionAccessControlIT extends IntegrationTestBase {

    @Test
    void user_cannot_fetch_someone_elses_transaction_support_can() {
        UUID user1 = UUID.randomUUID();
        UUID user2 = UUID.randomUUID();
        UUID support = UUID.randomUUID();
        UUID tx1 = UUID.randomUUID();
        UUID tx2 = UUID.randomUUID();

        ingestUser(user1, "u1", "pw1", List.of("ROLE_USER"));
        ingestUser(user2, "u2", "pw2", List.of("ROLE_USER"));
        ingestUser(support, "support", "pw3", List.of("ROLE_SUPPORT"));

        ingestTransaction(Map.ofEntries(
                Map.entry("transactionId", tx1.toString()),
                Map.entry("userId", user1.toString()),
                Map.entry("postedAt", Instant.now().minusSeconds(3600).toString()),
                Map.entry("amount", new BigDecimal("10.50")),
                Map.entry("currency", "ZAR"),
                Map.entry("merchant", "ACME"),
                Map.entry("status", "POSTED"),
                Map.entry("bin", "411111"),
                Map.entry("last4digits", "1111"),
                Map.entry("panHash", "hash1"),
                Map.entry("rrn", "rrn-1")
        ));

        ingestTransaction(Map.ofEntries(
                Map.entry("transactionId", tx2.toString()),
                Map.entry("userId", user2.toString()),
                Map.entry("postedAt", Instant.now().minusSeconds(7200).toString()),
                Map.entry("amount", new BigDecimal("99.99")),
                Map.entry("currency", "ZAR"),
                Map.entry("merchant", "OTHER"),
                Map.entry("status", "POSTED"),
                Map.entry("bin", "400000"),
                Map.entry("last4digits", "0002"),
                Map.entry("panHash", "hash2"),
                Map.entry("rrn", "rrn-2")
        ));

        String userToken = loginAndGetToken("u1", "pw1");
        String supportToken = loginAndGetToken("support", "pw3");

        // user can read own
        webClient.get()
                .uri("/v1/transactions/" + tx1)
                .headers(h -> h.setBearerAuth(userToken))
                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectBody()
                .jsonPath("$.data.transactionId").isEqualTo(tx1.toString());

        // user must not read someone else's (404 by design)
        webClient.get()
                .uri("/v1/transactions/" + tx2)
                .headers(h -> h.setBearerAuth(userToken))
                .exchange()
                .expectStatus().isNotFound();

        // support can read any
        webClient.get()
                .uri("/v1/transactions/" + tx2)
                .headers(h -> h.setBearerAuth(supportToken))
                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectBody()
                .jsonPath("$.data.transactionId").isEqualTo(tx2.toString());
    }
}
