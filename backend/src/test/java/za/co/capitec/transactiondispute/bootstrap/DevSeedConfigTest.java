package za.co.capitec.transactiondispute.bootstrap;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import reactor.core.publisher.Mono;
import za.co.capitec.transactiondispute.security.application.PasswordService;
import za.co.capitec.transactiondispute.security.application.port.out.UserRepositoryPort;
import za.co.capitec.transactiondispute.security.domain.model.UserAccount;
import za.co.capitec.transactiondispute.transactions.application.config.TransactionsModuleProperties;
import za.co.capitec.transactiondispute.transactions.application.port.out.TransactionRepositoryPort;
import za.co.capitec.transactiondispute.transactions.domain.model.TransactionStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class DevSeedConfigTest {

    @Test
    void seedUsers_whenDisabled_doesNothing() {
        var cfg = new DevSeedConfig();
        var props = new AppSeedProperties(false, List.of(new AppSeedProperties.UserSeed(
                UUID.randomUUID(), "bob", "pw", List.of("ROLE_USER")
        )));
        var users = mock(UserRepositoryPort.class);
        var passwords = mock(PasswordService.class);

        cfg.seedUsers(props, users, passwords).onApplicationEvent(mock(ApplicationReadyEvent.class));

        verifyNoInteractions(users, passwords);
    }

    @Test
    void seedUsers_whenUserMissing_hashesAndInserts() {
        var cfg = new DevSeedConfig();
        var userId = UUID.randomUUID();

        var props = new AppSeedProperties(true, List.of(new AppSeedProperties.UserSeed(
                userId, "bob", "pw", List.of("ROLE_ADMIN")
        )));
        var users = mock(UserRepositoryPort.class);
        var passwords = mock(PasswordService.class);

        when(users.findByUsername("bob")).thenReturn(Mono.empty());
        when(passwords.hash("pw")).thenReturn(Mono.just("hashed"));
        when(users.insert(any())).thenReturn(Mono.empty());

        cfg.seedUsers(props, users, passwords).onApplicationEvent(mock(ApplicationReadyEvent.class));

        var captor = ArgumentCaptor.forClass(UserAccount.class);
        verify(users).insert(captor.capture());
        assertThat(captor.getValue().userId()).isEqualTo(userId);
        assertThat(captor.getValue().username()).isEqualTo("bob");
        assertThat(captor.getValue().passwordHash()).isEqualTo("hashed");
        assertThat(captor.getValue().roles()).containsExactly("ROLE_ADMIN");
    }

    @Test
    void seedTransactionsUsers_whenNoSeed_doesNothing() {
        var cfg = new DevSeedConfig();
        var props = new TransactionsModuleProperties(List.of());
        var repo = mock(TransactionRepositoryPort.class);

        cfg.seedTransactionsUsers(props, repo).onApplicationEvent(mock(ApplicationReadyEvent.class));

        verifyNoInteractions(repo);
    }

    @Test
    void seedTransactionsUsers_whenSeed_present_callsUpsertAll() {
        var cfg = new DevSeedConfig();
        var txId = UUID.randomUUID();
        var userId = UUID.randomUUID();

        var props = new TransactionsModuleProperties(List.of(new TransactionsModuleProperties.Transaction(
                txId,
                userId,
                Instant.parse("2026-02-01T00:00:00Z"),
                BigDecimal.TEN,
                "ZAR",
                "Merchant",
                TransactionStatus.POSTED,
                "123456",
                "1234",
                "panhash",
                "rrn"
        )));

        var repo = mock(TransactionRepositoryPort.class);
        when(repo.upsertAll(any())).thenReturn(Mono.empty());

        cfg.seedTransactionsUsers(props, repo).onApplicationEvent(mock(ApplicationReadyEvent.class));

        verify(repo).upsertAll(any());
    }
}