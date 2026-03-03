package za.co.capitec.transactiondispute.shared.infrastructure.http;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.reactivestreams.Publisher;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.security.MessageDigest;
import java.time.Duration;
import java.util.Base64;
import java.util.List;

public class IdempotencyWebFilter implements WebFilter {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyWebFilter.class);

    public static final String HEADER_IDEMPOTENCY_KEY = "X-Idempotency-Key";

    private static final String PREFIX_IN_PROGRESS = "IN_PROGRESS";
    private static final String PREFIX_COMPLETED = "COMPLETED";

    private final ReactiveStringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final IdempotencyProperties props;

    public IdempotencyWebFilter(ReactiveStringRedisTemplate redis,
                                ObjectMapper objectMapper,
                                IdempotencyProperties props) {
        this.redis = redis;
        this.objectMapper = objectMapper;
        this.props = props == null ? IdempotencyProperties.defaults() : props;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        if (!Boolean.TRUE.equals(props.enabled())) {
            return chain.filter(exchange);
        }

        ServerHttpRequest request = exchange.getRequest();
        HttpMethod method = request.getMethod();
        if (!isMutating(method)) {
            return chain.filter(exchange);
        }

        String path = request.getPath().pathWithinApplication().value();
        if (isExcludedPath(path)) {
            return chain.filter(exchange);
        }

        String idempotencyKey = request.getHeaders().getFirst(HEADER_IDEMPOTENCY_KEY);
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            exchange.getResponse().setStatusCode(HttpStatus.BAD_REQUEST);
            return exchange.getResponse().setComplete();
        }

        return resolveUserKey(exchange)
                .flatMap(userKey -> cacheRequestBodyAndHash(exchange)
                        .flatMap(cached -> handleIdempotency(exchange, chain, userKey, idempotencyKey, cached))
                );
    }

    private Mono<String> resolveUserKey(ServerWebExchange exchange) {
        return exchange.getPrincipal()
                .cast(Authentication.class)
                .map(Authentication::getName)
                .defaultIfEmpty("anonymous");
    }

    private Mono<CachedRequest> cacheRequestBodyAndHash(ServerWebExchange exchange) {
        ServerHttpRequest request = exchange.getRequest();

        return DataBufferUtils.join(request.getBody())
                .defaultIfEmpty(exchange.getResponse().bufferFactory().wrap(new byte[0]))
                .flatMap(buf -> {
                    byte[] bytes = new byte[buf.readableByteCount()];
                    buf.read(bytes);
                    DataBufferUtils.release(buf);

                    String reqHash = sha256Hex(normalizeJson(bytes));

                    Flux<DataBuffer> cachedBody = Flux.defer(() -> {
                        DataBufferFactory f = exchange.getResponse().bufferFactory();
                        return Mono.just(f.wrap(bytes));
                    });

                    ServerHttpRequest decorated = new ServerHttpRequestDecorator(request) {
                        @Override
                        public Flux<DataBuffer> getBody() {
                            return cachedBody;
                        }
                    };

                    return Mono.just(new CachedRequest(decorated, reqHash));
                });
    }

    private byte[] normalizeJson(byte[] bytes) {
        try {
            if (bytes.length == 0) return bytes;
            Object tree = objectMapper.readValue(bytes, Object.class);
            return objectMapper.writeValueAsBytes(tree);
        } catch (Exception _) {
            return bytes;
        }
    }

    private Mono<Void> handleIdempotency(ServerWebExchange exchange,
                                         WebFilterChain chain,
                                         String userKey,
                                         String idempotencyKey,
                                         CachedRequest cached) {
        ServerHttpRequest request = cached.request();
        String path = request.getPath().pathWithinApplication().value();
        String method = request.getMethod().name();

        String redisKey = buildRedisKey(method, path, userKey, idempotencyKey);
        String inProgressValue = PREFIX_IN_PROGRESS + ":" + cached.requestHash();

        Duration inProgressTtl = saneTtl(props.inProgressTtl(), Duration.ofMinutes(1));
        Duration completedTtl = saneTtl(props.completedTtl(), Duration.ofMinutes(10));

        return redis.opsForValue()
                .setIfAbsent(redisKey, inProgressValue, inProgressTtl)
                .flatMap(acquired -> Boolean.TRUE.equals(acquired)
                        ? proceedWithLock(exchange, chain, cached, redisKey, completedTtl)
                        : handleNotAcquired(exchange, chain, cached, redisKey)
                )
                .onErrorResume(ex -> {
                    log.warn("Redis unavailable; bypassing idempotency (fail-open). key={}", redisKey, ex);
                    return chain.filter(exchange.mutate().request(cached.request()).build());
                });
    }

    private Mono<Void> proceedWithLock(ServerWebExchange exchange,
                                       WebFilterChain chain,
                                       CachedRequest cached,
                                       String redisKey,
                                       Duration completedTtl) {
        return proceedAndCapture(exchange, chain, cached.request(), redisKey, cached.requestHash(), completedTtl)
                .onErrorResume(ex ->
                        redis.delete(redisKey)
                                .onErrorResume(ignore -> Mono.empty())
                                .then(Mono.error(ex))
                );
    }

    private Mono<Void> handleNotAcquired(ServerWebExchange exchange,
                                         WebFilterChain chain,
                                         CachedRequest cached,
                                         String redisKey) {
        return redis.opsForValue()
                .get(redisKey)
                .flatMap(existing -> handleExistingValue(exchange, chain, cached, existing))
                .onErrorResume(ex -> {
                    log.warn("Redis unavailable; bypassing idempotency read (fail-open). key={}", redisKey, ex);
                    return chain.filter(exchange.mutate().request(cached.request()).build());
                });
    }

    private Mono<Void> handleExistingValue(ServerWebExchange exchange,
                                           WebFilterChain chain,
                                           CachedRequest cached,
                                           String existing) {
        if (existing == null || existing.isBlank()) {
            // Key disappeared between SETNX miss and GET; just let it proceed normally.
            return filter(exchange.mutate().request(cached.request()).build(), chain);
        }

        if (existing.startsWith(PREFIX_COMPLETED + ":")) {
            return handleCompleted(exchange, cached, existing);
        }

        if (existing.startsWith(PREFIX_IN_PROGRESS + ":")) {
            return handleInProgress(exchange, cached, existing);
        }

        return conflict(exchange);
    }

    private Mono<Void> handleCompleted(ServerWebExchange exchange,
                                       CachedRequest cached,
                                       String existing) {
        IdempotencyCompleted completed = decodeCompleted(existing);
        if (completed == null) {
            return conflict(exchange);
        }
        if (!completed.requestHash().equals(cached.requestHash())) {
            return conflict(exchange);
        }
        return replay(exchange, completed);
    }

    private Mono<Void> handleInProgress(ServerWebExchange exchange,
                                        CachedRequest cached,
                                        String existing) {
        String existingHash = existing.substring((PREFIX_IN_PROGRESS + ":").length());

        // If the key is reused with a different payload while a request is in progress → conflict.
        if (!existingHash.equals(cached.requestHash())) {
            return conflict(exchange);
        }

        // Same key + same payload but still in progress → conflict (409), by your contract/tests.
        return conflict(exchange);
    }

    private Duration saneTtl(Duration ttl, Duration fallback) {
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            return fallback;
        }
        return ttl;
    }

    private Mono<Void> proceedAndCapture(ServerWebExchange exchange,
                                         WebFilterChain chain,
                                         ServerHttpRequest cachedRequest,
                                         String redisKey,
                                         String reqHash,
                                         Duration completedTtl) {

        ServerHttpResponse originalResponse = exchange.getResponse();
        DataBufferFactory bufferFactory = originalResponse.bufferFactory();

        ServerHttpResponseDecorator decoratedResponse = new ServerHttpResponseDecorator(originalResponse) {

            @Override
            public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
                return Flux.from(body)
                        .reduce(new java.io.ByteArrayOutputStream(), (baos, dataBuffer) -> {
                            byte[] bytes = new byte[dataBuffer.readableByteCount()];
                            dataBuffer.read(bytes);
                            DataBufferUtils.release(dataBuffer);
                            try {
                                baos.write(bytes);
                            } catch (Exception _) {
                                // ByteArrayOutputStream#write does not throw in practice.
                                // We intentionally ignore to preserve original body bytes.
                            }
                            return baos;
                        })
                        .flatMap(baos -> {
                            byte[] all = baos.toByteArray();

                            var sc = getStatusCode();
                            int statusCode = sc != null ? sc.value() : HttpStatus.OK.value();

                            MediaType contentType = getHeaders().getContentType();
                            String ct = contentType == null ? MediaType.APPLICATION_JSON_VALUE : contentType.toString();

                            String bodyB64 = Base64.getEncoder().encodeToString(all);
                            IdempotencyCompleted completed = new IdempotencyCompleted(reqHash, statusCode, ct, bodyB64);
                            String encoded = encodeCompleted(completed);
                            if (encoded == null) {
                                return super.writeWith(Mono.just(bufferFactory.wrap(all)));
                            }

                            // IMPORTANT: store COMPLETED and only then write the response
                            return redis.opsForValue()
                                    .set(redisKey, encoded, completedTtl)
                                    .onErrorResume(ex -> {
                                        log.warn("Redis unavailable; could not store COMPLETED idempotency result (fail-open). key={}", redisKey, ex);
                                        return Mono.empty();
                                    }).then(Mono.defer(() -> super.writeWith(Mono.just(bufferFactory.wrap(all)))));
                        });
            }

            @Override
            public Mono<Void> writeAndFlushWith(Publisher<? extends Publisher<? extends DataBuffer>> body) {
                return writeWith(Flux.from(body).flatMap(p -> p));
            }
        };

        ServerWebExchange mutated = exchange.mutate()
                .request(cachedRequest)
                .response(decoratedResponse)
                .build();

        return chain.filter(mutated)
                .onErrorResume(ex ->
                        redis.delete(redisKey)
                                .onErrorResume(ignore -> Mono.empty())
                                .then(Mono.error(ex))
                );
    }

    private Mono<Void> replay(ServerWebExchange exchange, IdempotencyCompleted completed) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.valueOf(completed.status()));
        response.getHeaders().set(HttpHeaders.CONTENT_TYPE, completed.contentType());

        byte[] body = Base64.getDecoder().decode(completed.bodyBase64());
        if (body.length == 0) {
            return response.setComplete();
        }
        DataBuffer buffer = response.bufferFactory().wrap(body);
        return response.writeWith(Mono.just(buffer));
    }

    private Mono<Void> conflict(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.CONFLICT);
        return exchange.getResponse().setComplete();
    }

    private String buildRedisKey(String method, String path, String userKey, String idempotencyKey) {
        String userPart = sha256Hex(userKey == null ? new byte[0] : userKey.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        return "idem:" + method + ":" + path + ":" + userPart + ":" + idempotencyKey;
    }

    private boolean isMutating(HttpMethod method) {
        return method == HttpMethod.POST || method == HttpMethod.PUT || method == HttpMethod.PATCH || method == HttpMethod.DELETE;
    }

    private boolean isExcludedPath(String path) {
        return startsWithAny(path, List.of(
                "/actuator",
                "/swagger-ui",
                "/v3/api-docs",
                "/webjars",
                "/auth",
                "/internal",
                "/v1/ingest"
        ));
    }

    private boolean startsWithAny(String path, List<String> prefixes) {
        for (String p : prefixes) {
            if (path.startsWith(p)) {
                return true;
            }
        }
        return false;
    }

    private String sha256Hex(byte[] bytes) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(bytes);
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16));
                sb.append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (Exception _) {
            return "";
        }
    }

    private String encodeCompleted(IdempotencyCompleted completed) {
        try {
            return PREFIX_COMPLETED + ":" + objectMapper.writeValueAsString(completed);
        } catch (JsonProcessingException _) {
            return null;
        }
    }

    private IdempotencyCompleted decodeCompleted(String raw) {
        try {
            String json = raw.substring((PREFIX_COMPLETED + ":").length());
            return objectMapper.readValue(json, IdempotencyCompleted.class);
        } catch (Exception _) {
            return null;
        }
    }

    private record CachedRequest(ServerHttpRequest request, String requestHash) {}

    private record IdempotencyCompleted(
            String requestHash,
            int status,
            String contentType,
            String bodyBase64
    ) {}
}