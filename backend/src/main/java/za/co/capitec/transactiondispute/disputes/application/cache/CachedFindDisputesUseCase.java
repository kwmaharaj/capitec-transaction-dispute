package za.co.capitec.transactiondispute.disputes.application.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Mono;
import za.co.capitec.transactiondispute.disputes.application.port.in.FindDisputesUseCase;
import za.co.capitec.transactiondispute.disputes.domain.model.Dispute;
import za.co.capitec.transactiondispute.disputes.domain.model.DisputeStatus;
import za.co.capitec.transactiondispute.shared.infrastructure.cache.CacheKeyFactory;
import za.co.capitec.transactiondispute.shared.infrastructure.cache.CacheNames;
import za.co.capitec.transactiondispute.shared.infrastructure.cache.ReactiveCacheAside;

import java.util.Objects;

public class CachedFindDisputesUseCase implements FindDisputesUseCase {

    private final FindDisputesUseCase delegate;
    private final ReactiveCacheAside cache;
    private final CacheKeyFactory keys;

    public CachedFindDisputesUseCase(FindDisputesUseCase delegate, ReactiveCacheAside cache, CacheKeyFactory keys) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.cache = Objects.requireNonNull(cache, "cache");
        this.keys = Objects.requireNonNull(keys, "keys");
    }

    @Override
    public Mono<Page<Dispute>> findByDisputeStatus(DisputeStatus status, Pageable pageable) {
        String hash = keys.hash(new KeyParts(status, pageable));
        return cache.getOrLoad(
                CacheNames.DISPUTES_SUPPORT,
                keys.versioned(CacheNames.DISPUTES_SUPPORT, hash),
                new TypeReference<>() {},
                () -> delegate.findByDisputeStatus(status, pageable)
        );
    }

    public Mono<Void> invalidate() {
        return keys.bumpNamespaceVersion(CacheNames.DISPUTES_SUPPORT).then();
    }

    private record KeyParts(DisputeStatus status, Pageable pageable) {}
}
