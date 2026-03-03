package za.co.capitec.transactiondispute.bootstrap;

import org.springframework.boot.context.properties.ConfigurationProperties;

/*
Allows us to isolate connection per module. When split this can get can'd to use yaml per module
See *R2dbcReposConfig in relevant modules and app.postgres
 */
@ConfigurationProperties(prefix = "app.postgres")
public record PostgresModulesProperties(
        String host,
        int port,
        int connectionTimeout,
        int statementTimeout,
        String database,
        Module security,
        Module transactions,
        Module disputes
) {
    public record Module(String username, String password, String schema) {}
}