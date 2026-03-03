package za.co.capitec.transactiondispute.testsupport.interfaces.http;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Test-only endpoints used to validate global idempotency behaviour.
 *
 * Lives in test sources so it is never packaged in production.
 */
@RestController
@RequestMapping(path = "/v1/test/idempotency", produces = MediaType.APPLICATION_JSON_VALUE)
public class IdempotencyTestController {

    private final AtomicInteger executions = new AtomicInteger();

    @PostMapping("/fast")
    public Mono<Map<String, Object>> fast(@RequestHeader("X-Idempotency-Key") String idemKey,
                                          @RequestBody(required = false) Map<String, Object> body) {
        int exec = executions.incrementAndGet();

        // deterministic "resultId" derived from idempotency key
        UUID resultId = UUID.nameUUIDFromBytes(idemKey.getBytes(StandardCharsets.UTF_8));

        return Mono.just(Map.of(
                "execution", exec,
                "resultId", resultId.toString(),
                "echo", body == null ? Map.of() : body
        ));
    }

    @PostMapping("/slow")
    public Mono<Map<String, Object>> slow(@RequestBody(required = false) Map<String, Object> body) {
        int n = executions.incrementAndGet();
        return Mono.delay(Duration.ofMillis(400))
                .thenReturn(Map.of(
                        "execution", n,
                        "resultId", UUID.randomUUID().toString(),
                        "echo", body == null ? Map.of() : body
                ));
    }

    @GetMapping("/executions")
    public Mono<Map<String, Object>> executions() {
        return Mono.just(Map.of("count", executions.get()));
    }
}
