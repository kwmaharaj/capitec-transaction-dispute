package za.co.capitec.transactiondispute.security.application;

import org.springframework.security.crypto.password.PasswordEncoder;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Reactive wrapper around a {@link PasswordEncoder}.
 *
 * Password hashing/verification is CPU-bound, so we offload to boundedElastic
 * to keep Netty event-loop threads non-blocking.
 */
public class PasswordService {

    private final PasswordEncoder encoder;

    public PasswordService(PasswordEncoder encoder) {
        this.encoder = encoder;
    }

    public Mono<String> hash(String rawPassword) {
        return Mono.fromCallable(() -> encoder.encode(rawPassword))
                .subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<Boolean> matches(String rawPassword, String storedHash) {
        return Mono.fromCallable(() -> encoder.matches(rawPassword, storedHash))
                .subscribeOn(Schedulers.boundedElastic());
    }
}
