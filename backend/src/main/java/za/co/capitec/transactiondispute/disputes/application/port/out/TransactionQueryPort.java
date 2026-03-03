package za.co.capitec.transactiondispute.disputes.application.port.out;

import reactor.core.publisher.Mono;
import za.co.capitec.transactiondispute.disputes.application.model.DisputableTransactionView;

import java.util.UUID;

public interface TransactionQueryPort {
    Mono<DisputableTransactionView> findDisputableById(UUID transactionId);
}