package za.co.capitec.transactiondispute.bootstrap;

import org.flywaydb.core.Flyway;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * Per module Flyway configuration.
 *
 * Each module owns its own schema + its own Flyway history table, which makes later splitting later. db per module
 */
@Configuration
public class FlywayModulesConfig {

    private static DataSource ds(String jdbcUrl, PostgresModulesProperties.Module m) {
        return DataSourceBuilder.create()
                .url(jdbcUrl)
                .username(m.username())
                .password(m.password())
                .build();
    }

    @Bean
    public ApplicationRunner flywayRunner(PostgresModulesProperties p) {
        var jdbcUrl = "jdbc:postgresql://" + p.host() + ":" + p.port() + "/" + p.database();

        var securityFlyway = Flyway.configure()
                .dataSource(ds(jdbcUrl, p.security()))
                .schemas(p.security().schema())
                .table("flyway_schema_history_security")
                .locations("classpath:modules/security/db/migration")
                .createSchemas(false) //handled in root.docker.postgres.init
                .load();

        var transactionsFlyway = Flyway.configure()
                .dataSource(ds(jdbcUrl, p.transactions()))
                .schemas(p.transactions().schema())
                .table("flyway_schema_history_transactions")
                .locations("classpath:modules/transactions/db/migration")
                .createSchemas(false) //handled in root.docker.postgres.init
                .load();

        var disputesFlyway = Flyway.configure()
                .dataSource(ds(jdbcUrl, p.disputes()))
                .schemas(p.disputes().schema())
                .table("flyway_schema_history_disputes")
                .locations("classpath:modules/disputes/db/migration")
                .createSchemas(false) //handled in root.docker.postgres.init
                .load();

        return args -> {
            securityFlyway.migrate();
            transactionsFlyway.migrate();
            disputesFlyway.migrate();
        };
    }
}