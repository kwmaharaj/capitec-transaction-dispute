package za.co.capitec.transactiondispute.testsupport;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Base class for Testcontainers-backed WebFlux integration tests.
 *
 * Starts Postgres (with module roles/schemas) + Redis.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class IntegrationTestBase {

    @LocalServerPort
    int port;

    protected WebTestClient webClient;

    @BeforeEach
    void setupClient() {
        resetDatabaseAndRedis();
        this.webClient = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .responseTimeout(Duration.ofSeconds(10))
                .build();
    }
    private static void resetDatabaseAndRedis() {
        TestContainersSupport.resetDatabaseAndRedis();
    }


    @DynamicPropertySource
    static void registerProps(DynamicPropertyRegistry r) {
        r.add("app.postgres.host", TestContainersSupport.postgres()::getHost);
        r.add("app.postgres.port", () -> TestContainersSupport.postgres().getMappedPort(5432));
        r.add("app.postgres.database", () -> "txdispute");

        r.add("app.postgres.security.username", () -> "security_app");
        r.add("app.postgres.security.password", () -> "security_app_pw");
        r.add("app.postgres.security.schema", () -> "security");

        r.add("app.postgres.transactions.username", () -> "transactions_app");
        r.add("app.postgres.transactions.password", () -> "transactions_app_pw");
        r.add("app.postgres.transactions.schema", () -> "transactions");

        r.add("app.postgres.disputes.username", () -> "disputes_app");
        r.add("app.postgres.disputes.password", () -> "disputes_app_pw");
        r.add("app.postgres.disputes.schema", () -> "disputes");

        r.add("spring.data.redis.host", TestContainersSupport.redis()::getHost);
        r.add("spring.data.redis.port", () -> TestContainersSupport.redis().getMappedPort(6379));

        // Required for transaction module hashing; keep deterministic for tests.
        r.add("modules.transactions.security.pan-hash-secret", () -> "0123456789abcdef0123456789abcdef");
        // Keep JWT secret stable for tests.
        r.add("modules.security.jwt.secret", () -> "change-me-please-change-me-please-change-me");
        // Keep JWT issuer stable for tests.
        r.add("modules.security.jwt.issuer", () -> "http://transaction-dispute-service");

        //redis reset on each tests
        r.add("app.idempotency.enabled", () -> "true");
        r.add("app.idempotency.in-progress-ttl", () -> "PT1M");
        r.add("app.idempotency.completed-ttl", () -> "PT10M");

    }

    protected void ingestUser(UUID userId, String username, String password, List<String> roles) {
        webClient.post()
                .uri("/internal/ingest/users")
                .bodyValue(List.of(Map.of(
                        "userId", userId.toString(),
                        "username", username,
                        "password", password,
                        "roles", roles
                )))
                .exchange()
                .expectStatus().is2xxSuccessful();
    }

    protected void ingestTransaction(Map<String, Object> tx) {
        webClient.post()
                .uri("/internal/ingest/transactions")
                .bodyValue(List.of(tx))
                .exchange()
                .expectStatus().is2xxSuccessful();
    }

    protected String loginAndGetToken(String username, String password) {
        byte[] body = webClient.post()
                .uri("/auth/login")
                .bodyValue(Map.of("username", username, "password", password))
                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectBody()
                .returnResult()
                .getResponseBodyContent();

        if (body == null) {
            throw new IllegalStateException("Login response body was empty");
        }

        return JsonPath.read(new String(body), "$.data.accessToken");
    }
}