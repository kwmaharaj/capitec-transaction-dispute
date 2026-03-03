package za.co.capitec.transactiondispute.shared.infrastructure.cache;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Cache configuration (per endpoint) with sensible defaults.
 *
 * app:
 *   cache:
 *     default-ttl: PT5M
 *     ttl:
 *       tx-by-id: PT2M
 */
@ConfigurationProperties(prefix = "app.cache")
public class CacheProperties {

    /**
     * Default TTL used when an endpoint TTL is not configured.
     */
    private Duration defaultTtl = Duration.ofMinutes(5);

    /**
     * Per-endpoint TTLs.
     *
     * Key is an endpoint/cache name (see {@link CacheNames}).
     */
    private Map<String, Duration> ttl = new HashMap<>();

    public Duration getDefaultTtl() {
        return defaultTtl;
    }

    public void setDefaultTtl(Duration defaultTtl) {
        this.defaultTtl = defaultTtl;
    }

    public Map<String, Duration> getTtl() {
        return ttl;
    }

    public void setTtl(Map<String, Duration> ttl) {
        this.ttl = ttl;
    }

    public Duration ttlFor(String cacheName) {
        return ttl.getOrDefault(cacheName, defaultTtl);
    }
}
