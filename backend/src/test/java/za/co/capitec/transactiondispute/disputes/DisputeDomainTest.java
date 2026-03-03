package za.co.capitec.transactiondispute.disputes;

import org.junit.jupiter.api.Test;
import za.co.capitec.transactiondispute.disputes.domain.model.Dispute;
import za.co.capitec.transactiondispute.disputes.domain.model.DisputeStatus;
import za.co.capitec.transactiondispute.disputes.domain.model.ReasonCode;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DisputeDomainTest {

    @Test
    void open_dispute_starts_open_and_can_only_move_to_in_progress() {
        var now = Instant.now();
        var d = Dispute.openDispute(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), ReasonCode.OTHER, "note", now);

        assertThat(d.disputeStatus()).isEqualTo(DisputeStatus.OPEN);

        assertThatThrownBy(() -> d.decide(DisputeStatus.RESOLVED, "x"))
                .isInstanceOf(IllegalStateException.class);

        d.decide(DisputeStatus.IN_PROGRESS, "investigate");
        assertThat(d.disputeStatus()).isEqualTo(DisputeStatus.IN_PROGRESS);
        assertThat(d.note()).isEqualTo("investigate");
        assertThat(d.updatedAt()).isAfterOrEqualTo(now);
    }

    @Test
    void in_progress_can_transition_to_resolved_rejected_or_open() {
        var now = Instant.now();
        var d = Dispute.openDispute(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), ReasonCode.FRAUD_SUSPECTED, "note", now)
                .decide(DisputeStatus.IN_PROGRESS, "work");

        d.decide(DisputeStatus.OPEN, "re-open");
        assertThat(d.disputeStatus()).isEqualTo(DisputeStatus.OPEN);

        // go back to in-progress then resolve
        d.decide(DisputeStatus.IN_PROGRESS, "again");
        d.decide(DisputeStatus.RESOLVED, "done");
        assertThat(d.disputeStatus()).isEqualTo(DisputeStatus.RESOLVED);
    }
}
