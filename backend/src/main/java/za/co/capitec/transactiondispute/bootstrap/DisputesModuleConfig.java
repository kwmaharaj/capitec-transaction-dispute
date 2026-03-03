package za.co.capitec.transactiondispute.bootstrap;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.transaction.reactive.TransactionalOperator;
import za.co.capitec.transactiondispute.disputes.application.CreateDisputeService;
import za.co.capitec.transactiondispute.disputes.application.config.DisputesModuleProperties;
import za.co.capitec.transactiondispute.disputes.application.port.in.*;
import za.co.capitec.transactiondispute.disputes.application.port.out.*;
import za.co.capitec.transactiondispute.disputes.application.services.*;
import za.co.capitec.transactiondispute.disputes.infrastructure.messaging.LoggingDisputeEventPublisher;
import za.co.capitec.transactiondispute.disputes.infrastructure.persistence.*;
import za.co.capitec.transactiondispute.shared.integration.TransactionsTransactionQueryAdapter;
import za.co.capitec.transactiondispute.transactions.application.port.in.TransactionQueryUseCase;

import java.time.Clock;

@Configuration
@EnableConfigurationProperties(DisputesModuleProperties.class)
public class DisputesModuleConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public DisputeRepositoryPort disputeRepositoryPort(
            @Qualifier("disputesTemplate") R2dbcEntityTemplate disputesTemplate
    ) {
        return new SpringDataDisputeRepository(disputesTemplate);
    }

    @Bean
    public DisputeHistoryRepositoryPort disputeHistoryRepositoryPort(
            @Qualifier("disputesTemplate") R2dbcEntityTemplate disputesTemplate
    ) {
        return new DisputeHistoryTemplateRepository(disputesTemplate);
    }

    @Bean
    public DisputeViewRepositoryPort disputeViewRepositoryPort(
            @Qualifier("disputesDatabaseClient") DatabaseClient disputesDatabaseClient
    ) {
        return new DisputeViewDatabaseRepository(disputesDatabaseClient);
    }

    @Bean
    public DisputeEventPublisherPort disputeEventPublisherPort() {
        return new LoggingDisputeEventPublisher();
    }

    @Bean
    public TransactionQueryPort disputeTransactionQueryPort(TransactionQueryUseCase transactions) {
        return new TransactionsTransactionQueryAdapter(transactions);
    }

    @Bean
    public CreateDisputeUseCase createDisputeUseCase(TransactionQueryPort transactionQueryPort,
                                                     DisputeRepositoryPort disputeRepositoryPort,
                                                     DisputeHistoryRepositoryPort disputeHistoryRepositoryPort,
                                                     DisputeEventPublisherPort disputeEventPublisherPort,
                                                     @Qualifier("disputesTxOperator") TransactionalOperator disputesTxOperator,
                                                     Clock clock,
                                                     DisputesModuleProperties props) {
        return new CreateDisputeService(
                transactionQueryPort,
                disputeRepositoryPort,
                disputeHistoryRepositoryPort,
                disputeEventPublisherPort,
                disputesTxOperator,
                clock,
                props
        );
    }

    @Bean
    public FindUserDisputesUseCase findUserDisputesUseCase(DisputeRepositoryPort repo) {
        return new FindUserDisputesService(repo);
    }

    @Bean
    public FindDisputesUseCase findDisputesUseCase(DisputeRepositoryPort repo) {
        return new FindDisputesService(repo);
    }

    @Bean
    public DecideDisputeUseCase decideDisputeUseCase(DisputeRepositoryPort repo,
                                                     DisputeHistoryRepositoryPort history,
                                                     DisputeEventPublisherPort events,
                                                     @Qualifier("disputesTxOperator") TransactionalOperator disputesTxOperator,
                                                     Clock clock) {
        return new DecideDisputeUseCaseService(repo, history, events, disputesTxOperator, clock);
    }

    @Bean
    public DisputeHistoryUseCase disputeHistoryUseCase(DisputeRepositoryPort disputes,
                                                       DisputeHistoryRepositoryPort history) {
        return new DisputeHistoryService(disputes, history);
    }

    @Bean
    public DisputeViewQueryUseCase disputeViewQueryUseCase(DisputeViewRepositoryPort repo) {
        return new DisputeViewQueryService(repo);
    }
}
