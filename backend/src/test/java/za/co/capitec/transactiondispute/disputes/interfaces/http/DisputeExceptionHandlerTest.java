package za.co.capitec.transactiondispute.disputes.interfaces.http;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import za.co.capitec.transactiondispute.disputes.application.exceptions.*;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DisputeExceptionHandlerTest {

    private final DisputeExceptionHandler handler = new DisputeExceptionHandler();

    @Test
    void handleTransactionNotFound_sets404TitleAndTransactionId() {
        var txId = UUID.randomUUID();

        var pd = handler.handle(new TransactionNotFoundException(txId));

        assertThat(pd.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(pd.getTitle()).isEqualTo("Transaction not found");
        assertThat(pd.getProperties()).containsEntry("transactionId", txId);
    }

    @Test
    void handleOpenDisputeAlreadyExists_sets409TitleAndTransactionId() {
        var txId = UUID.randomUUID();

        var pd = handler.handle(new OpenDisputeAlreadyExistsException(txId));

        assertThat(pd.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(pd.getTitle()).isEqualTo("Dispute already exists");
        assertThat(pd.getProperties()).containsEntry("transactionId", txId);
    }

    @Test
    void handleNotOwned_mapsToNotFoundLikeNotFound() {
        var txId = UUID.randomUUID();
        var userId = UUID.randomUUID();

        var pd = handler.handle(new TransactionNotOwnedException(txId, userId));

        assertThat(pd.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(pd.getTitle()).isEqualTo("Transaction not found");
        assertThat(pd.getProperties()).containsEntry("transactionId", txId);
    }

    @Test
    void handleNotDisputable_setsConflictWithStatusProperty() {
        var txId = UUID.randomUUID();

        var pd = handler.handle(new TransactionNotDisputableException(txId, "DECLINED"));

        assertThat(pd.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(pd.getTitle()).isEqualTo("Transaction not disputable");
        assertThat(pd.getProperties()).containsEntry("transactionId", txId);
        assertThat(pd.getProperties()).containsEntry("transactionStatus", "DECLINED");
    }

    @Test
    void handleWindowExpired_setsConflictWithPostedAtAndMaxAge() {
        var txId = UUID.randomUUID();
        var postedAt = Instant.parse("2026-02-01T00:00:00Z");

        var pd = handler.handle(new TransactionDisputeWindowExpiredException(txId, postedAt, 60));

        assertThat(pd.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(pd.getTitle()).isEqualTo("Dispute window expired");
        assertThat(pd.getProperties()).containsEntry("transactionId", txId);
        assertThat(pd.getProperties()).containsEntry("postedAt", postedAt);
        assertThat(pd.getProperties()).containsEntry("maxAgeDays", 60);
    }

    @Test
    void handleBadRequestAndInvalidState_haveExpectedStatus() {
        var badReq = handler.handleBadRequest(new IllegalArgumentException("bad"));
        assertThat(badReq.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(badReq.getDetail()).isEqualTo("bad");

        var invalid = handler.handleInvalidState(new IllegalStateException("nope"));
        assertThat(invalid.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(invalid.getDetail()).isEqualTo("nope");
    }

    @Test
    void handleDisputeNotFound_sets404() {
        var pd = handler.handleNotFound(new DisputeNotFoundException(UUID.randomUUID()));

        assertThat(pd.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(pd.getDetail()).contains("not found");
    }
}