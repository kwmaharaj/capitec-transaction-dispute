package za.co.capitec.transactiondispute.disputes.interfaces.http;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import za.co.capitec.transactiondispute.disputes.application.exceptions.*;


@RestControllerAdvice
public class DisputeExceptionHandler {

    private static final String TRANSACTION_ID = "transactionId";

    @ExceptionHandler(TransactionNotFoundException.class)
    public ProblemDetail handle(TransactionNotFoundException ex) {
        var pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        pd.setTitle("Transaction not found");
        pd.setProperty(TRANSACTION_ID, ex.transactionId());
        return pd;
    }

    @ExceptionHandler(OpenDisputeAlreadyExistsException.class)
    public ProblemDetail handle(OpenDisputeAlreadyExistsException ex) {
        var pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        pd.setTitle("Dispute already exists");
        pd.setProperty(TRANSACTION_ID, ex.transactionId());
        return pd;
    }

    @ExceptionHandler(DisputeNotFoundException.class)
    public ProblemDetail handleNotFound(DisputeNotFoundException ex) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        pd.setDetail(ex.getMessage());
        return pd;
    }

    @ExceptionHandler(IllegalStateException.class)
    public ProblemDetail handleInvalidState(IllegalStateException ex) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        pd.setDetail(ex.getMessage());
        return pd;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleBadRequest(IllegalArgumentException ex) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        pd.setDetail(ex.getMessage());
        return pd;
    }

    @ExceptionHandler(TransactionNotOwnedException.class)
    public ProblemDetail handle(TransactionNotOwnedException ex) {
        var pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        pd.setTitle("Transaction not found");
        pd.setProperty(TRANSACTION_ID, ex.transactionId());
        return pd;
    }

    @ExceptionHandler(TransactionNotDisputableException.class)
    public ProblemDetail handle(TransactionNotDisputableException ex) {
        var pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        pd.setTitle("Transaction not disputable");
        pd.setProperty(TRANSACTION_ID, ex.transactionId());
        pd.setProperty("transactionStatus", ex.status());
        return pd;
    }

    @ExceptionHandler(TransactionDisputeWindowExpiredException.class)
    public ProblemDetail handle(TransactionDisputeWindowExpiredException ex) {
        var pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        pd.setTitle("Dispute window expired");
        pd.setProperty(TRANSACTION_ID, ex.transactionId());
        pd.setProperty("postedAt", ex.postedAt());
        pd.setProperty("maxAgeDays", ex.maxAgeDays());
        return pd;
    }
}
