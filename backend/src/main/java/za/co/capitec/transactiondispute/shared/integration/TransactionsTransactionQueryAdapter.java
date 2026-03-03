package za.co.capitec.transactiondispute.shared.integration;

import reactor.core.publisher.Mono;
import za.co.capitec.transactiondispute.disputes.application.model.DisputableTransactionView;
import za.co.capitec.transactiondispute.disputes.application.port.out.TransactionQueryPort;
import za.co.capitec.transactiondispute.disputes.domain.model.TransactionStatus;
import za.co.capitec.transactiondispute.transactions.application.model.TransactionView;
import za.co.capitec.transactiondispute.transactions.application.port.in.TransactionQueryUseCase;

import java.util.UUID;

/**
 * Integration adapter that lets the disputes module query the transactions module
 * without introducing a compile-time dependency from disputes -> transactions.
 * If deployed seperate can swop out for http/messaging
 */
public class TransactionsTransactionQueryAdapter implements TransactionQueryPort {

    private final TransactionQueryUseCase transactions;

    public TransactionsTransactionQueryAdapter(TransactionQueryUseCase transactions) {
        this.transactions = transactions;
    }

    @Override
    public Mono<DisputableTransactionView> findDisputableById(UUID transactionId) {
        return transactions.findById(transactionId)
                .map(TransactionsTransactionQueryAdapter::toDisputable);
    }

    private static DisputableTransactionView toDisputable(TransactionView t) {
        // NOTE: accountId isn't implemented in the transactions module yet.
        // For now, we reuse userId as the "account" surrogate.
        return new DisputableTransactionView(
                t.transactionId(),
                t.userId(),
                t.postedAt(),
                t.amount(),
                t.currency(),
                t.merchant(),
                TransactionStatus.valueOf(t.transactionStatus().name())
        );
    }
}
