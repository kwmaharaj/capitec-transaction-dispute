package za.co.capitec.transactiondispute.disputes.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class DisputeDecideRulesTest {

    @Test
    void open_canOnlyGoToInProgress() {
        var d = Dispute.openDispute(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                ReasonCode.FRAUD_SUSPECTED,
                "note",
                Instant.parse("2026-02-01T00:00:00Z")
        );

        assertThatThrownBy(() -> d.decide(DisputeStatus.RESOLVED, "x"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("OPEN can only transition");

        assertThat(d.decide(DisputeStatus.IN_PROGRESS, "working").disputeStatus())
                .isEqualTo(DisputeStatus.IN_PROGRESS);
    }

    @Test
    void inProgress_canGoToResolvedRejectedOpen_only() {
        var d = Dispute.openDispute(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                ReasonCode.FRAUD_SUSPECTED,
                "note",
                Instant.parse("2026-02-01T00:00:00Z")
        );

        d.decide(DisputeStatus.IN_PROGRESS, "working");

        assertThatThrownBy(() -> d.decide(DisputeStatus.IN_PROGRESS, "nope"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("IN_PROGRESS can only transition");

        assertThat(d.decide(DisputeStatus.RESOLVED, "done").disputeStatus()).isEqualTo(DisputeStatus.RESOLVED);
    }
}