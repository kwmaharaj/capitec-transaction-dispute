package za.co.capitec.transactiondispute.shared.infrastructure.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Objects;

/**
 * Production-grade cache keys:
 * - stable prefix
 * - explicit namespaces
 * - user scoping where needed
 * - hashed suffixes for variable criteria to keep keys short and safe
 * - namespace versioning to enable cheap invalidation without SCAN
 */
public class CacheKeyFactory {

    private static final String PREFIX = "txdispute";

    private final ReactiveCacheStore store;
    private final ObjectMapper objectMapper;

    public CacheKeyFactory(ReactiveCacheStore store, ObjectMapper objectMapper) {
        this.store = Objects.requireNonNull(store, "store");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
    }

    public String simple(String... parts) {
        List<String> all = new ArrayList<>(parts.length + 1);
        all.add(PREFIX);
        all.addAll(java.util.Arrays.asList(parts));
        return String.join(":", all);
    }

    /**
     * Generates a key in the form:
     * txdispute:{namespace}:v{ver}:{suffix...}
     *
     * where ver is stored in Redis at txdispute:cachever:{namespace}.
     */
    public Mono<String> versioned(String namespace, String... suffixParts) {
        String versionKey = simple("cachever", namespace);

        return store.get(versionKey)
                .map(v -> {
                    try {
                        return Long.parseLong(v);
                    } catch (Exception _) {
                        return 0L;
                    }
                })
                .onErrorReturn(0L) // fail-open
                .defaultIfEmpty(0L)
                .map(ver -> {
                    String[] all = new String[suffixParts.length + 3];
                    all[0] = PREFIX;
                    all[1] = namespace;
                    all[2] = "v" + ver;
                    System.arraycopy(suffixParts, 0, all, 3, suffixParts.length);
                    return String.join(":", all);
                });
    }

    public Mono<Long> bumpNamespaceVersion(String namespace) {
        String versionKey = simple("cachever", namespace);
        return store.increment(versionKey)
                .onErrorReturn(0L); // fail-open
    }

    public String hash(Object value) {
        try {
            String json = objectMapper.writeValueAsString(value);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(json.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hashed);
        } catch (Exception _) {
            // If hashing fails, fall back to a coarse key (still deterministic for same toString).
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                byte[] hashed = digest.digest(String.valueOf(value).getBytes(StandardCharsets.UTF_8));
                return Base64.getUrlEncoder().withoutPadding().encodeToString(hashed);
            } catch (Exception _) {
                return "hasherr";
            }
        }
    }
}
