package za.co.capitec.transactiondispute.disputes.application.port.in;

import reactor.core.publisher.Mono;
import za.co.capitec.transactiondispute.disputes.application.model.CreateDisputeCommand;
import za.co.capitec.transactiondispute.disputes.application.model.CreateDisputeResult;

public interface CreateDisputeUseCase {

    Mono<CreateDisputeResult> create(CreateDisputeCommand command);

}