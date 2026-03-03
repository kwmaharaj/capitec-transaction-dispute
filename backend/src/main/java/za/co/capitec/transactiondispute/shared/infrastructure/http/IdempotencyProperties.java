package za.co.capitec.transactiondispute.shared.infrastructure.http;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * idempotency settings for mutating HTTP requests.
 */
@ConfigurationProperties(prefix = "app.idempotency")
public record IdempotencyProperties(
        Boolean enabled,
        Duration inProgressTtl,
        Duration completedTtl
) {
    public IdempotencyProperties {
        // default to ON unless explicitly disabled
        if (enabled == null) enabled = true;

        // default TTLs if omitted
        if (inProgressTtl == null || inProgressTtl.isZero() || inProgressTtl.isNegative()) {
            inProgressTtl = Duration.ofMinutes(1);
        }
        if (completedTtl == null || completedTtl.isZero() || completedTtl.isNegative()) {
            completedTtl = Duration.ofMinutes(10);
        }
    }

    public static IdempotencyProperties defaults() {
        return new IdempotencyProperties(true, Duration.ofMinutes(1), Duration.ofMinutes(10));
    }
}