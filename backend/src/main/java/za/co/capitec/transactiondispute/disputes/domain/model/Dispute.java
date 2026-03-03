package za.co.capitec.transactiondispute.disputes.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class Dispute {

    private final UUID disputeId;
    private final UUID transactionId;
    private final UUID userId;
    private final ReasonCode reasonCode;
    private final Instant createdAt;

    //can change
    private String note;
    private DisputeStatus disputeStatus;
    private Instant updatedAt;

    private Dispute(UUID disputeId,
                    UUID transactionId,
                    UUID userId,
                    ReasonCode reasonCode,
                    String note,
                    DisputeStatus disputeStatus,
                    AuditMetadata auditMetadata) {

        this.disputeId = Objects.requireNonNull(disputeId, "disputeId");
        this.transactionId = Objects.requireNonNull(transactionId, "transactionId");
        this.userId = Objects.requireNonNull(userId, "userId");
        this.reasonCode = Objects.requireNonNull(reasonCode, "reasonCode");
        this.note = note; // required when dispute create or dispute status changes.
        this.disputeStatus = Objects.requireNonNull(disputeStatus, "disputeStatus");
        this.createdAt = Objects.requireNonNull(auditMetadata.createdAt(), "createdAt");
        this.updatedAt = Objects.requireNonNull(auditMetadata.updatedAt(), "updatedAt");
    }

    /**
     * used when support changing status
     * @param newStatus
     * @param newNote
     * @return
     */
    public Dispute decide(DisputeStatus newStatus, String newNote) {

        Objects.requireNonNull(newStatus, "status");
        Objects.requireNonNull(newNote, "note");

        if (this.disputeStatus == DisputeStatus.OPEN &&
                newStatus != DisputeStatus.IN_PROGRESS) {

            throw new IllegalStateException(
                    "OPEN can only transition to IN_PROGRESS"
            );
        }

        if (this.disputeStatus == DisputeStatus.IN_PROGRESS &&
                newStatus != DisputeStatus.RESOLVED &&
                newStatus != DisputeStatus.REJECTED &&
                newStatus != DisputeStatus.OPEN) {

            throw new IllegalStateException(
                    "IN_PROGRESS can only transition to RESOLVED, REJECTED, or OPEN"
            );
        }

        this.disputeStatus = newStatus;
        this.note = newNote;
        this.updatedAt = Instant.now();

        return this;
    }

    /**
     * new is created as OPEN
     * @param disputeId
     * @param transactionId
     * @param userId
     * @param reasonCode
     * @param note
     * @param now
     * @return
     */
    public static Dispute openDispute(UUID disputeId,
                                      UUID transactionId,
                                      UUID userId,
                                      ReasonCode reasonCode,
                                      String note,
                                      Instant now) {
        return new Dispute(
                disputeId,
                transactionId,
                userId,
                reasonCode,
                note,
                DisputeStatus.OPEN,
                new AuditMetadata(now,now)
        );
    }

    /**
     * Persistence rehydration factory.
     * Keeps the constructor private. To allow infrastructure adapters to rebuild aggregate state.
     */
    public static Dispute rehydrate(UUID disputeId,
                                    UUID transactionId,
                                    UUID userId,
                                    ReasonCode reasonCode,
                                    String note,
                                    DisputeStatus status,
                                    AuditMetadata auditMetadata) {
        return new Dispute(
                disputeId,
                transactionId,
                userId,
                reasonCode,
                note,
                status,
                auditMetadata
        );
    }

    public UUID disputeId() { return disputeId; }
    public UUID transactionId() { return transactionId; }
    public UUID userId() { return userId; }
    public ReasonCode reasonCode() { return reasonCode; }
    public String note() { return note; }
    public DisputeStatus disputeStatus() { return disputeStatus; }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }
}