package za.co.capitec.transactiondispute.bootstrap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import za.co.capitec.transactiondispute.security.application.PasswordService;
import za.co.capitec.transactiondispute.security.application.port.out.UserRepositoryPort;
import za.co.capitec.transactiondispute.security.domain.model.UserAccount;
import za.co.capitec.transactiondispute.transactions.application.config.TransactionsModuleProperties;
import za.co.capitec.transactiondispute.transactions.application.port.out.TransactionRepositoryPort;
import za.co.capitec.transactiondispute.transactions.domain.model.Transaction;

import java.util.List;

/**
 * Dev/demo-only seeding.
 *
 * Run with: --spring.profiles.active=dev
 */
@Configuration
@Profile("dev")
@EnableConfigurationProperties(AppSeedProperties.class)
public class DevSeedConfig {

    private static final Logger LOG = LoggerFactory.getLogger(DevSeedConfig.class);

    @Bean
    public ApplicationListener<ApplicationReadyEvent> seedUsers(AppSeedProperties seedProps,
                                                               UserRepositoryPort users,
                                                               PasswordService passwords) {
        return event -> {
            if (!seedProps.enabled()) {
                return;
            }

            var seeds = seedProps.users();
            if (seeds == null || seeds.isEmpty()) {
                return;
            }

            Flux.fromIterable(seeds)
                    .concatMap(seed -> ensureSeedUser(seed, users, passwords))
                    .doOnError(e -> LOG.error("User seed failed:", e))
                    .subscribe();
        };
    }

    private Mono<Void> ensureSeedUser(AppSeedProperties.UserSeed seed,
                                     UserRepositoryPort users,
                                     PasswordService passwords) {
        List<String> roles = (seed.roles() == null || seed.roles().isEmpty())
                ? List.of("ROLE_USER")
                : seed.roles();

        return users.findByUsername(seed.username())
                .flatMap(existing -> Mono.<Void>empty())
                .switchIfEmpty(
                        passwords.hash(seed.password())
                                .flatMap(hash -> users.insert(new UserAccount(
                                        seed.userId(),
                                        seed.username(),
                                        hash,
                                        roles
                                )))
                )
                .doOnSuccess(v -> LOG.info("Seeded user '{}' (or already existed)", seed.username()));
    }



    /**
     * seed txs for testing for users, see yaml
     * @param props
     * @param repo
     * @return
     */
    @Bean
    public ApplicationListener<ApplicationReadyEvent> seedTransactionsUsers(TransactionsModuleProperties props, TransactionRepositoryPort repo) {
        return event -> {
            var seed = props.transactionsToSeed();
            if (seed == null || seed.isEmpty()) {
                return;
            }

            var initial = seed.stream()
                    .map(t -> new Transaction(
                            t.transactionId(),
                            t.userId(),
                            t.postedAt(),
                            t.amount(),
                            t.currency(),
                            t.merchant(),
                            t.transactionStatus(),
                            t.bin(),
                            t.last4digits(),
                            t.panHash(),
                            t.rrn()
                    ))
                    .toList();

            repo.upsertAll(Flux.fromIterable(initial))
                    .doOnError(e -> LOG.error("Transaction seed failed:", e))
                    .subscribe();
        };
    }
}
