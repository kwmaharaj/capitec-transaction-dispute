package za.co.capitec.transactiondispute.bootstrap;

import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryOptions;
import io.r2dbc.spi.Option;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.r2dbc.connection.R2dbcTransactionManager;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.transaction.reactive.TransactionalOperator;

import java.time.Duration;

import static io.r2dbc.spi.ConnectionFactoryOptions.*;

/**
 * modularised
 */
@Configuration
class R2dbcModuleConnectionFactories {

    private ConnectionFactory cf(PostgresModulesProperties p, PostgresModulesProperties.Module m) {
        var opts = ConnectionFactoryOptions.builder()
                .option(DRIVER, "postgresql")
                .option(HOST, p.host())
                .option(PORT, p.port())
                .option(DATABASE, p.database())
                .option(Option.valueOf("connectTimeout"), Duration.ofSeconds(p.connectionTimeout()))
                .option(Option.valueOf("statementTimeout"), Duration.ofSeconds(p.statementTimeout()))
                .option(USER, m.username())
                .option(PASSWORD, m.password())
                .option(Option.valueOf("schema"), m.schema()) // IMPORTANT!!! connection is bound to each db scheme
                .build();
        return io.r2dbc.spi.ConnectionFactories.get(opts);
    }

    // conn factories modularised
    @Bean
    ConnectionFactory securityConnectionFactory(PostgresModulesProperties p) {
        return cf(p, p.security());
    }

    @Bean
    ConnectionFactory transactionsConnectionFactory(PostgresModulesProperties p) {
        return cf(p, p.transactions());
    }

    @Bean
    ConnectionFactory disputesConnectionFactory(PostgresModulesProperties p) {
        return cf(p, p.disputes());
    }

    // templates
    @Bean
    R2dbcEntityTemplate securityTemplate(ConnectionFactory securityConnectionFactory) {
        return new R2dbcEntityTemplate(securityConnectionFactory);
    }

    @Bean
    R2dbcEntityTemplate transactionsTemplate(ConnectionFactory transactionsConnectionFactory) {
        return new R2dbcEntityTemplate(transactionsConnectionFactory);
    }

    @Bean
    R2dbcEntityTemplate disputesTemplate(ConnectionFactory disputesConnectionFactory) {
        return new R2dbcEntityTemplate(disputesConnectionFactory);
    }

    /**
     * Module-scoped db clients.
     * Do not expose a single global DatabaseClient or a global spring.r2dbc.* config.
     * Each module must wire explicitly to its own schema-bound ConnectionFactory!!!
     */
    // db clients
    @Bean
    DatabaseClient securityDatabaseClient(ConnectionFactory securityConnectionFactory) {
        return DatabaseClient.builder().connectionFactory(securityConnectionFactory).build();
    }

    @Bean
    DatabaseClient transactionsDatabaseClient(ConnectionFactory transactionsConnectionFactory) {
        return DatabaseClient.builder().connectionFactory(transactionsConnectionFactory).build();
    }

    @Bean
    DatabaseClient disputesDatabaseClient(ConnectionFactory disputesConnectionFactory) {
        return DatabaseClient.builder().connectionFactory(disputesConnectionFactory).build();
    }

    @Bean
    public R2dbcTransactionManager disputesTxManager(
            @Qualifier("disputesConnectionFactory") ConnectionFactory cf) {
        return new R2dbcTransactionManager(cf);
    }

    @Bean
    public TransactionalOperator disputesTxOperator(
            @Qualifier("disputesTxManager") R2dbcTransactionManager tm) {
        return TransactionalOperator.create(tm);
    }

    @Bean
    public R2dbcTransactionManager transactionsTxManager(
            @Qualifier("transactionsConnectionFactory") ConnectionFactory cf) {
        return new R2dbcTransactionManager(cf);
    }

    @Bean
    public TransactionalOperator transactionsTxOperator(
            @Qualifier("transactionsTxManager") R2dbcTransactionManager tm) {
        return TransactionalOperator.create(tm);
    }

    @Bean
    public R2dbcTransactionManager securityTxManager(
            @Qualifier("securityConnectionFactory") ConnectionFactory cf) {
        return new R2dbcTransactionManager(cf);
    }

    @Bean
    public TransactionalOperator securityTxOperator(
            @Qualifier("securityTxManager") R2dbcTransactionManager tm) {
        return TransactionalOperator.create(tm);
    }
}