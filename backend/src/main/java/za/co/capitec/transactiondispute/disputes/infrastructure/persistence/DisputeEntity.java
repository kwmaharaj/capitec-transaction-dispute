package za.co.capitec.transactiondispute.disputes.infrastructure.persistence;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import za.co.capitec.transactiondispute.disputes.domain.model.DisputeStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * R2DBC entity for disputes.disputes.
 *
 * NOTE: We use Persistable so Spring Data can decide INSERT vs UPDATE when IDs are assigned by the domain.
 */
@Table(schema = "disputes", name = "disputes")
public class DisputeEntity implements Persistable<UUID> {

    @Id
    @Column("dispute_id")
    private UUID disputeId;

    @Column("transaction_id")
    private UUID transactionId;

    @Column("user_id")
    private UUID userId;

    @Column("reason_code")
    private String reasonCode;

    @Column("note")
    private String note;

    @Column("dispute_status")
    private DisputeStatus disputeStatus;

    @Column("created_at")
    private Instant createdAt;

    @Column("updated_at")
    private Instant updatedAt;

    @Transient
    private boolean isNew;

    public DisputeEntity() {
        // for Spring Data
    }

    public DisputeEntity(UUID disputeId,
                        UUID transactionId,
                        UUID userId,
                        String reasonCode,
                        String note,
                        DisputeStatus disputeStatus,
                         DisputeAudit disputeAudit) {
        this.disputeId = disputeId;
        this.transactionId = transactionId;
        this.userId = userId;
        this.reasonCode = reasonCode;
        this.note = note;
        this.disputeStatus = disputeStatus;
        this.createdAt = disputeAudit.createdAt();
        this.updatedAt = disputeAudit.updatedAt();
    }

    @Override
    public UUID getId() {
        return disputeId;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    public DisputeEntity markNew(boolean isNew) {
        this.isNew = isNew;
        return this;
    }
    public UUID getTransactionId() {
        return transactionId;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getReasonCode() {
        return reasonCode;
    }

    public String getNote() {
        return note;
    }

    public DisputeStatus getDisputeStatus() {
        return disputeStatus;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
