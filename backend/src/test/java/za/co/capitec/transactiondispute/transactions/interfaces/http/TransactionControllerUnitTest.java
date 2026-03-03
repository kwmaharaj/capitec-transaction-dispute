package za.co.capitec.transactiondispute.transactions.interfaces.http;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import za.co.capitec.transactiondispute.transactions.application.exceptions.TransactionNotFoundException;
import za.co.capitec.transactiondispute.transactions.application.model.TransactionView;
import za.co.capitec.transactiondispute.transactions.application.port.in.TransactionQueryUseCase;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class TransactionControllerUnitTest {

    private TransactionQueryUseCase queries;
    private TransactionController controller;

    @BeforeEach
    void setUp() {
        queries = mock(TransactionQueryUseCase.class);
        controller = new TransactionController(queries);
    }


    @Test
    void findById_user_filtersByOwner_returns404WhenNotOwned() {
        var auth = mock(Authentication.class);
        var userId = UUID.randomUUID();
        when(auth.getName()).thenReturn(userId.toString());
        when(auth.getAuthorities()).thenReturn(List.of()); // not support

        var txId = UUID.randomUUID();
        var otherUsersTx = new TransactionView(
                txId,
                UUID.randomUUID(), // different owner
                null, null, null, null, null, null, null, null, null
        );

        when(queries.findById(txId)).thenReturn(Mono.just(otherUsersTx));

        StepVerifier.create(controller.findById(auth, txId))
                .expectErrorSatisfies(ex -> {
                    assertThat(ex).isInstanceOf(TransactionNotFoundException.class);
                })
                .verify();
    }
}