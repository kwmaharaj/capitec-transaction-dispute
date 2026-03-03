package za.co.capitec.transactiondispute.disputes.application.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import za.co.capitec.transactiondispute.disputes.application.port.in.FindUserDisputesUseCase;
import za.co.capitec.transactiondispute.disputes.domain.model.Dispute;
import za.co.capitec.transactiondispute.shared.infrastructure.cache.CacheKeyFactory;
import za.co.capitec.transactiondispute.shared.infrastructure.cache.CacheNames;
import za.co.capitec.transactiondispute.shared.infrastructure.cache.ReactiveCacheAside;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class CachedFindUserDisputesUseCase implements FindUserDisputesUseCase {

    private final FindUserDisputesUseCase delegate;
    private final ReactiveCacheAside cache;
    private final CacheKeyFactory keys;

    public CachedFindUserDisputesUseCase(FindUserDisputesUseCase delegate,
                                         ReactiveCacheAside cache,
                                         CacheKeyFactory keys) {
        this.delegate = Objects.requireNonNull(delegate);
        this.cache = Objects.requireNonNull(cache);
        this.keys = Objects.requireNonNull(keys);
    }

    @Override
    public Flux<Dispute> findByUserId(UUID userId) {
        String key = keys.simple(CacheNames.DISPUTES_BY_USER, userId.toString());

        Mono<List<DisputeCacheDto>> cached = cache.getOrLoad(
                CacheNames.DISPUTES_BY_USER,
                Mono.just(key),
                new TypeReference<List<DisputeCacheDto>>() {},
                () -> delegate.findByUserId(userId)
                        .map(DisputeCacheDto::fromDomain)
                        .collectList()
        );

        return cached
                .map(list -> list.stream()
                        .map(DisputeCacheDto::toDomain)
                        .toList()
                )
                .flatMapMany(Flux::fromIterable);
    }

    public Mono<Void> invalidate(UUID userId) {
        String key = keys.simple(CacheNames.DISPUTES_BY_USER, userId.toString());
        return cache.deleteBestEffort(key);
    }

    /**
     * Cache-safe DTO (simple structure, Jackson-friendly)
     */
    static class DisputeCacheDto {
        private UUID id;
        private UUID transactionId;
        private UUID userId;
        private String reasonCode;
        private String notes;
        private String createdAt;

        public DisputeCacheDto() {
            // Intentionally empty – required for Jackson deserialization. Comment is for S1186 or use suppress annotation
        }

        public UUID getId() { return id; }
        public void setId(UUID id) { this.id = id; }

        public UUID getTransactionId() { return transactionId; }
        public void setTransactionId(UUID transactionId) { this.transactionId = transactionId; }

        public UUID getUserId() { return userId; }
        public void setUserId(UUID userId) { this.userId = userId; }

        public String getReasonCode() { return reasonCode; }
        public void setReasonCode(String reasonCode) { this.reasonCode = reasonCode; }

        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }

        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

        static DisputeCacheDto fromDomain(Dispute d) {
            DisputeCacheDto dto = new DisputeCacheDto();
            dto.setId(d.disputeId());
            dto.setTransactionId(d.transactionId());
            dto.setUserId(d.userId());
            dto.setReasonCode(d.reasonCode().name());
            dto.setNotes(d.note());
            dto.setCreatedAt(d.createdAt().toString());
            return dto;
        }

        Dispute toDomain() {
            return Dispute.openDispute(
                    id,
                    transactionId,
                    userId,
                    Enum.valueOf(
                            za.co.capitec.transactiondispute.disputes.domain.model.ReasonCode.class,
                            reasonCode
                    ),
                    notes,
                    java.time.Instant.parse(createdAt)
            );
        }
    }
}