package za.co.capitec.transactiondispute.disputes.infrastructure.persistence;

import java.util.Objects;
import java.util.UUID;

public record DisputeActor(String role, UUID userId) {
    public DisputeActor {
        Objects.requireNonNull(role, "role");
        Objects.requireNonNull(userId, "userId");
    }
}