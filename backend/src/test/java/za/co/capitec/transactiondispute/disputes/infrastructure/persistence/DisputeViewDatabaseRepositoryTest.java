package za.co.capitec.transactiondispute.disputes.infrastructure.persistence;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import za.co.capitec.transactiondispute.TransactionDisputeServiceApplication;
import za.co.capitec.transactiondispute.disputes.application.model.DisputeSearchCriteria;
import za.co.capitec.transactiondispute.disputes.application.model.DisputeTransactionViewRow;
import za.co.capitec.transactiondispute.disputes.application.port.out.DisputeViewRepositoryPort;
import za.co.capitec.transactiondispute.disputes.domain.model.DisputeStatus;
import za.co.capitec.transactiondispute.disputes.domain.model.TransactionStatus;
import za.co.capitec.transactiondispute.testsupport.TestContainersSupport;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = TransactionDisputeServiceApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
class DisputeViewDatabaseRepositoryTest {

    @Autowired
    DisputeViewRepositoryPort repo;

    @Autowired
    @Qualifier("transactionsDatabaseClient")
    DatabaseClient transactionsDb;

    @Autowired
    @Qualifier("disputesDatabaseClient")
    DatabaseClient disputesDb;

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
    void searchForUser_nullCriteria_filtersByUser_ordersByCreatedAtDesc_andPages() {
        UUID user1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID user2 = UUID.fromString("00000000-0000-0000-0000-000000000002");

        UUID tx1 = UUID.fromString("10000000-0000-0000-0000-000000000001");
        UUID tx2 = UUID.fromString("10000000-0000-0000-0000-000000000002");
        UUID txOtherUser = UUID.fromString("10000000-0000-0000-0000-000000000003");

        Instant postedAt = Instant.parse("2026-01-01T10:00:00Z");

        UUID dOld = UUID.fromString("20000000-0000-0000-0000-000000000001");
        UUID dNew = UUID.fromString("20000000-0000-0000-0000-000000000002");
        UUID dOtherUser = UUID.fromString("20000000-0000-0000-0000-000000000003");

        Instant createdOld = Instant.parse("2026-01-01T12:00:00Z");
        Instant createdNew = Instant.parse("2026-01-02T12:00:00Z");

        Mono<Void> setup = Mono.when(
                insertTransaction(tx1, user1, postedAt, new BigDecimal("10.00"), "ZAR", "Shoprite", TransactionStatus.POSTED, "123456", "1234", "h1", "RRN-111"),
                insertTransaction(tx2, user1, postedAt, new BigDecimal("20.00"), "ZAR", "Pick n Pay", TransactionStatus.PENDING, "654321", "9876", "h2", "RRN-222"),
                insertTransaction(txOtherUser, user2, postedAt, new BigDecimal("30.00"), "ZAR", "Woolworths", TransactionStatus.POSTED, "111111", "0000", "h3", "RRN-333"),

                insertDispute(dOld, tx1, user1, DisputeStatus.OPEN, "RC1", "note1", createdOld),
                insertDispute(dNew, tx2, user1, DisputeStatus.IN_PROGRESS, "RC2", "note2", createdNew),
                insertDispute(dOtherUser, txOtherUser, user2, DisputeStatus.OPEN, "RC3", "note3", createdNew)
        ).then();

        StepVerifier.create(setup.then(repo.searchForUser(user1, null, PageRequest.of(0, 10))))
                .assertNext(page -> {
                    assertThat(page.getTotalElements()).isEqualTo(2);
                    assertThat(page.getContent()).hasSize(2);

                    // ordered by d.created_at DESC
                    assertThat(page.getContent().getFirst().disputeId()).isEqualTo(dNew);
                    assertThat(page.getContent().getLast().disputeId()).isEqualTo(dOld);

                    DisputeTransactionViewRow first = page.getContent().getFirst();
                    assertThat(first.merchant()).isEqualTo("Pick n Pay");
                    assertThat(first.transactionStatus()).isEqualTo(TransactionStatus.PENDING);
                })
                .verifyComplete();
    }

    @Test
    void searchForSupport_withCombinedFilters_returnsOnlyMatchingRow_andCountsCorrectly() {
        UUID userA = UUID.fromString("00000000-0000-0000-0000-0000000000AA");
        UUID userB = UUID.fromString("00000000-0000-0000-0000-0000000000BB");

        UUID txMatch = UUID.fromString("10000000-0000-0000-0000-00000000AAAA");
        UUID txNoMatch = UUID.fromString("10000000-0000-0000-0000-00000000BBBB");
        UUID txOther = UUID.fromString("10000000-0000-0000-0000-00000000CCCC");

        Instant postedMatch = Instant.parse("2026-02-01T10:00:00Z");
        Instant postedOther = Instant.parse("2026-02-10T10:00:00Z");

        UUID dMatch = UUID.fromString("20000000-0000-0000-0000-00000000AAAA");
        UUID dNoMatch = UUID.fromString("20000000-0000-0000-0000-00000000BBBB");
        UUID dOther = UUID.fromString("20000000-0000-0000-0000-00000000CCCC");

        Instant createdAt = Instant.parse("2026-02-15T12:00:00Z");

        Mono<Void> setup = Mono.when(
                insertTransaction(txMatch, userA, postedMatch, new BigDecimal("99.99"), "ZAR", "Amazon Marketplace", TransactionStatus.POSTED, "123456", "1234", "h1", "RRN-XYZ-001"),
                insertTransaction(txNoMatch, userA, postedMatch, new BigDecimal("10.00"), "ZAR", "Amazon Marketplace", TransactionStatus.POSTED, "123456", "1234", "h2", "RRN-XYZ-002"),
                insertTransaction(txOther, userB, postedOther, new BigDecimal("99.99"), "ZAR", "Takealot", TransactionStatus.POSTED, "123456", "1234", "h3", "RRN-XYZ-001"),

                insertDispute(dMatch, txMatch, userA, DisputeStatus.OPEN, "RC1", "note1", createdAt),
                insertDispute(dNoMatch, txNoMatch, userA, DisputeStatus.REJECTED, "RC2", "note2", createdAt),
                insertDispute(dOther, txOther, userB, DisputeStatus.OPEN, "RC3", "note3", createdAt)
        ).then();

        // Ensure we hit: disputeId filter, addLike (merchant+rrn), addEqEnum (both enums),
        // addAmountRange (min+max), addDateRange (from+to).
        DisputeSearchCriteria criteria = new DisputeSearchCriteria(
                dMatch,
                new BigDecimal("50.00"),
                new BigDecimal("150.00"),
                "xyz-001",
                "amazon",
                DisputeStatus.OPEN,
                TransactionStatus.POSTED,
                Instant.parse("2026-02-01T00:00:00Z"),
                Instant.parse("2026-02-02T23:59:59Z")
        );

        StepVerifier.create(setup.then(repo.searchForSupport(criteria, PageRequest.of(0, 5))))
                .assertNext(page -> {
                    assertThat(page.getTotalElements()).isEqualTo(1);
                    assertThat(page.getContent()).hasSize(1);
                    assertThat(page.getContent().getFirst().disputeId()).isEqualTo(dMatch);
                    assertThat(page.getContent().getFirst().merchant()).containsIgnoringCase("amazon");
                    assertThat(page.getContent().getFirst().rrn()).isEqualTo("RRN-XYZ-001");
                })
                .verifyComplete();
    }

    private Mono<Void> insertTransaction(
            UUID transactionId,
            UUID userId,
            Instant postedAt,
            BigDecimal amount,
            String currency,
            String merchant,
            TransactionStatus status,
            String bin,
            String last4digits,
            String panHash,
            String rrn
    ) {
        return transactionsDb.sql("""
                        INSERT INTO transactions.transactions (
                            transaction_id, user_id, posted_at, amount, currency, merchant, transaction_status,
                            bin, last4digits, pan_hash, rrn, created_at, updated_at
                        ) VALUES (
                            :transaction_id, :user_id, :posted_at, :amount, :currency, :merchant, :transaction_status,
                            :bin, :last4digits, :pan_hash, :rrn, NOW(), NOW()
                        )
                        """)
                .bind("transaction_id", transactionId)
                .bind("user_id", userId)
                .bind("posted_at", postedAt)
                .bind("amount", amount)
                .bind("currency", currency)
                .bind("merchant", merchant)
                .bind("transaction_status", status.name())
                .bind("bin", bin)
                .bind("last4digits", last4digits)
                .bind("pan_hash", panHash)
                .bind("rrn", rrn)
                .fetch()
                .rowsUpdated()
                .then();
    }

    private Mono<Void> insertDispute(
            UUID disputeId,
            UUID transactionId,
            UUID userId,
            DisputeStatus status,
            String reasonCode,
            String note,
            Instant createdAt
    ) {
        return disputesDb.sql("""
                        INSERT INTO disputes.disputes (
                            dispute_id, transaction_id, user_id, reason_code, note, dispute_status,
                            version, created_at, updated_at, decided_at, decided_by
                        ) VALUES (
                            :dispute_id, :transaction_id, :user_id, :reason_code, :note, :dispute_status,
                            0, :created_at, :updated_at, NULL, NULL
                        )
                        """)
                .bind("dispute_id", disputeId)
                .bind("transaction_id", transactionId)
                .bind("user_id", userId)
                .bind("reason_code", reasonCode)
                .bind("note", note)
                .bind("dispute_status", status.name())
                .bind("created_at", createdAt)
                .bind("updated_at", createdAt)
                .fetch()
                .rowsUpdated()
                .then();
    }
}