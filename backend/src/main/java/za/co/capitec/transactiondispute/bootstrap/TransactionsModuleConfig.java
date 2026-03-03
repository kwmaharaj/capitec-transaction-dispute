package za.co.capitec.transactiondispute.bootstrap;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import za.co.capitec.transactiondispute.shared.infrastructure.cache.CacheKeyFactory;
import za.co.capitec.transactiondispute.shared.infrastructure.cache.ReactiveCacheAside;
import za.co.capitec.transactiondispute.transactions.application.*;
import za.co.capitec.transactiondispute.transactions.application.config.TransactionsModuleProperties;
import za.co.capitec.transactiondispute.transactions.application.port.in.TransactionIngestionUseCase;
import za.co.capitec.transactiondispute.transactions.application.port.in.TransactionQueryUseCase;
import za.co.capitec.transactiondispute.transactions.application.port.out.TransactionRepositoryPort;
import za.co.capitec.transactiondispute.transactions.infrastructure.persistence.SpringDataTemplateTransactionRepository;

@Configuration
@EnableConfigurationProperties(TransactionsModuleProperties.class)
public class TransactionsModuleConfig {

    @Bean
    public TransactionRepositoryPort transactionRepositoryPort(
            @Qualifier("transactionsTemplate") R2dbcEntityTemplate transactionsTemplate
    ) {
        return new SpringDataTemplateTransactionRepository(transactionsTemplate);
    }

    @Bean("transactionQueryUseCaseDelegate")
    public TransactionQueryUseCase transactionQueryUseCaseDelegate(TransactionRepositoryPort repo) {
        return new TransactionQueryService(repo);
    }

    @Bean
    @Primary
    public CachedTransactionQueryUseCase transactionQueryUseCase(
            @Qualifier("transactionQueryUseCaseDelegate") TransactionQueryUseCase delegate,
            ReactiveCacheAside cache,
            CacheKeyFactory keys
    ) {
        return new CachedTransactionQueryUseCase(delegate, cache, keys);
    }

    @Bean("transactionIngestionUseCaseDelegate")
    public TransactionIngestionUseCase transactionIngestionUseCaseDelegate(TransactionRepositoryPort repo) {
        return new TransactionIngestionService(repo);
    }

    @Bean
    @Primary
    public TransactionIngestionUseCase transactionIngestionUseCase(
            @Qualifier("transactionIngestionUseCaseDelegate") TransactionIngestionUseCase delegate,
            CachedTransactionQueryUseCase txCache
    ) {
        return new CachedTransactionIngestionUseCase(delegate, txCache);
    }
}
