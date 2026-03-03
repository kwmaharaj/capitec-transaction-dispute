package za.co.capitec.transactiondispute.transactions.interfaces.http;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import za.co.capitec.transactiondispute.transactions.application.exceptions.TransactionNotFoundException;

@RestControllerAdvice
public class TransactionExceptionHandler {

    @ExceptionHandler(TransactionNotFoundException.class)
    public ProblemDetail handle(TransactionNotFoundException ex) {
        var pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        pd.setTitle("Transaction not found");
        pd.setProperty("transactionId", ex.transactionId());
        return pd;
    }
}