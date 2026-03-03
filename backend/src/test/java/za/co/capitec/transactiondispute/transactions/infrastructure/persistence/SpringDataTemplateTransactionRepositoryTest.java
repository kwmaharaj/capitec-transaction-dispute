package za.co.capitec.transactiondispute.transactions.infrastructure.persistence;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;
import za.co.capitec.transactiondispute.TransactionDisputeServiceApplication;
import za.co.capitec.transactiondispute.testsupport.TestContainersSupport;
import za.co.capitec.transactiondispute.transactions.application.model.TransactionSearchCriteria;
import za.co.capitec.transactiondispute.transactions.application.port.out.TransactionRepositoryPort;
import za.co.capitec.transactiondispute.transactions.domain.model.Transaction;
import za.co.capitec.transactiondispute.transactions.domain.model.TransactionStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = TransactionDisputeServiceApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
class SpringDataTemplateTransactionRepositoryTest {

    @Autowired
    TransactionRepositoryPort repo;

    @BeforeEach
    void reset() {
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

        // deterministic secrets required by other beans at startup
        r.add("modules.transactions.security.pan-hash-secret", () -> "0123456789abcdef0123456789abcdef");
        r.add("modules.security.jwt.secret", () -> "change-me-please-change-me-please-change-me");
        r.add("modules.security.jwt.issuer", () -> "http://transaction-dispute-service");
    }

    @Test
    void upsertAll_inserts_thenUpdatesSameTransactionId() {
        UUID userId = UUID.fromString("00000000-0000-0000-0000-0000000000CC");
        UUID txId = UUID.fromString("10000000-0000-0000-0000-0000000000CC");

        Transaction original = new Transaction(
                txId,
                userId,
                Instant.parse("2026-02-01T10:00:00Z"),
                new BigDecimal("10.00"),
                "ZAR",
                "Shoprite",
                TransactionStatus.POSTED,
                "123456",
                "1234",
                "pan-hash-1",
                "RRN-1"
        );

        Transaction updated = new Transaction(
                txId,
                userId,
                Instant.parse("2026-02-02T10:00:00Z"),
                new BigDecimal("12.50"),
                "ZAR",
                "Pick n Pay",
                TransactionStatus.SETTLED,
                "123456",
                "1234",
                "pan-hash-2",
                "RRN-2"
        );

        StepVerifier.create(repo.upsertAll(Flux.just(original))
                        .then(repo.findById(txId)))
                .assertNext(found -> {
                    assertThat(found).isNotNull();
                    assertThat(found.merchant()).isEqualTo("Shoprite");
                    assertThat(found.amount()).isEqualByComparingTo("10.00");
                    assertThat(found.transactionStatus()).isEqualTo(TransactionStatus.POSTED);
                })
                .verifyComplete();

        StepVerifier.create(repo.upsertAll(Flux.just(updated))
                        .then(repo.findById(txId)))
                .assertNext(found -> {
                    assertThat(found).isNotNull();
                    assertThat(found.merchant()).isEqualTo("Pick n Pay");
                    assertThat(found.amount()).isEqualByComparingTo("12.50");
                    assertThat(found.transactionStatus()).isEqualTo(TransactionStatus.SETTLED);
                    assertThat(found.rrn()).isEqualTo("RRN-2");
                })
                .verifyComplete();
    }

    @Test
    void searchForUser_appliesCriteria_andReturnsPageWithTotal() {
        UUID userId = UUID.fromString("00000000-0000-0000-0000-0000000000DD");
        UUID tx1 = UUID.fromString("10000000-0000-0000-0000-0000000000D1");
        UUID tx2 = UUID.fromString("10000000-0000-0000-0000-0000000000D2");

        Transaction a = new Transaction(
                tx1, userId,
                Instant.parse("2026-01-01T10:00:00Z"),
                new BigDecimal("50.00"),
                "ZAR",
                "Amazon Marketplace",
                TransactionStatus.POSTED,
                "123456", "1111", "h1", "RRN-XYZ-001"
        );
        Transaction b = new Transaction(
                tx2, userId,
                Instant.parse("2026-01-03T10:00:00Z"),
                new BigDecimal("10.00"),
                "ZAR",
                "Takealot",
                TransactionStatus.POSTED,
                "123456", "2222", "h2", "RRN-ABC-999"
        );

        TransactionSearchCriteria c = new TransactionSearchCriteria(
                new BigDecimal("40.00"),
                null,
                "XYZ",
                "Amazon",
                TransactionStatus.POSTED,
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-01-02T23:59:59Z")
        );

        StepVerifier.create(repo.upsertAll(Flux.just(a, b))
                .then(repo.searchForUser(userId, c, PageRequest.of(0, 10))))
                .assertNext(page -> {
                    assertThat(page.getTotalElements()).isEqualTo(1);
                    assertThat(page.getContent()).hasSize(1);
                    assertThat(page.getContent().getFirst().transactionId()).isEqualTo(tx1);
                })
                .verifyComplete();
    }
}