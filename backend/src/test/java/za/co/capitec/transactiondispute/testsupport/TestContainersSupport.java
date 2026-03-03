package za.co.capitec.transactiondispute.testsupport;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.utility.DockerImageName;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Single, shared Testcontainers environment for the whole test JVM.
 *
 * Why:
 * - Spring caches ApplicationContexts between test classes (same configuration).
 * - JUnit @Testcontainers stops/starts containers per test class, which breaks cached contexts (ports change).
 * - Result: intermittent "cannot connect" failures depending on test ordering.
 *
 * We deliberately start containers once and keep them for the entire test run.
 */
public final class TestContainersSupport {
    private static final JdbcDatabaseContainer<?> POSTGRES =  new PostgreSQLContainer("postgres:18")
                    .withDatabaseName("txdispute")
                    .withUsername("postgres")
                    .withPassword("postgres")
                    .withInitScript("testcontainers/postgres-init.sql");


    private static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7"))
            .withExposedPorts(6379);

    static {
        POSTGRES.start();
        REDIS.start();
    }

    private TestContainersSupport() {}

    public static JdbcDatabaseContainer<?> postgres() {
        return POSTGRES;
    }

    public static GenericContainer<?> redis() {
        return REDIS;
    }

    /**
     * Deterministically clears state across all schemas (except Flyway history).
     *
     * This is intentionally JDBC-based and only used in tests.
     * It avoids brittle "truncate a hard-coded list of tables that might not exist yet".
     */
    public static void resetDatabaseAndRedis() {
        resetPostgres();
        resetRedis();
    }

    private static void resetPostgres() {
        try (Connection c = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             Statement s = c.createStatement()) {

            // Discover *existing* tables per schema and truncate them.
            // This avoids failures when table sets evolve.
            var rs = s.executeQuery("""
                    select schemaname, tablename
                    from pg_tables
                    where schemaname in ('security', 'transactions', 'disputes')
                      and tablename not like 'flyway_schema_history_%'
                    order by schemaname, tablename
                    """);
            List<String> tables = new ArrayList<>();
            while (rs.next()) {
                String schema = rs.getString("schemaname");
                String table = rs.getString("tablename");
                tables.add(schema + "." + table);
            }

            if (!tables.isEmpty()) {
                String joined = String.join(", ", tables);
                s.execute("TRUNCATE TABLE " + joined + " RESTART IDENTITY CASCADE");
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to reset Postgres test state", e);
        }
    }

    private static void resetRedis() {
        try {
            // Use redis-cli inside the container; avoids raw-socket RESP parsing issues.
            REDIS.execInContainer("redis-cli", "FLUSHALL");
        } catch (Exception e) {
            throw new IllegalStateException("Failed to reset Redis test state", e);
        }
    }
}
