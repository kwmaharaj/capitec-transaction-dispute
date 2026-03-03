package za.co.capitec.transactiondispute.disputes.infrastructure.persistence;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import za.co.capitec.transactiondispute.disputes.application.model.DisputeHistoryItem;
import za.co.capitec.transactiondispute.disputes.domain.model.DisputeStatus;

import java.time.Instant;
import java.util.UUID;

@Table(schema = "disputes", name = "dispute_history")
public class DisputeHistoryEntity {

    @Id
    @Column("history_id")
    private UUID historyId;

    @Column("dispute_id")
    private UUID disputeId;

    @Column("from_dispute_status")
    private DisputeStatus fromDisputeStatus;

    @Column("to_dispute_status")
    private DisputeStatus toDisputeStatus;

    @Column("note")
    private String note;

    @Column("actor_role")
    private String actorRole;

    @Column("actor_user_id")
    private UUID actorUserId;

    @Column("changed_at")
    private Instant changedAt;

    public DisputeHistoryEntity() {}

    public DisputeHistoryEntity(UUID historyId,
                                UUID disputeId,
                                DisputeStatus fromDisputeStatus,
                                DisputeStatus toDisputeStatus,
                                String note,
                                DisputeActor disputeActor,
                                Instant changedAt) {
        this.historyId = historyId;
        this.disputeId = disputeId;
        this.fromDisputeStatus = fromDisputeStatus;
        this.toDisputeStatus = toDisputeStatus;
        this.note = note;
        this.actorRole = disputeActor.role();
        this.actorUserId = disputeActor.userId();
        this.changedAt = changedAt;
    }

    public static DisputeHistoryEntity from(DisputeHistoryItem i) {
        return new DisputeHistoryEntity(
                i.historyId(),
                i.disputeId(),
                i.fromDisputeStatus(),
                i.toDisputeStatus(),
                i.note(),
                i.disputeActor(),
                i.changedAt()
        );
    }

    public DisputeHistoryItem toItem() {
        return new DisputeHistoryItem(
                historyId,
                disputeId,
                fromDisputeStatus,
                toDisputeStatus,
                note,
                new DisputeActor(actorRole,actorUserId),
                changedAt
        );
    }
}
