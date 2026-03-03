package za.co.capitec.transactiondispute;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import za.co.capitec.transactiondispute.disputes.application.port.in.CreateDisputeUseCase;
import za.co.capitec.transactiondispute.disputes.application.port.out.*;
import za.co.capitec.transactiondispute.security.application.AuthService;
import za.co.capitec.transactiondispute.security.application.JwtTokenService;
import za.co.capitec.transactiondispute.security.application.PasswordService;
import za.co.capitec.transactiondispute.security.application.TokenRevocationService;
import za.co.capitec.transactiondispute.security.application.port.in.UserIngestionUseCase;
import za.co.capitec.transactiondispute.security.application.port.out.UserRepositoryPort;
import za.co.capitec.transactiondispute.testsupport.IntegrationTestBase;
import za.co.capitec.transactiondispute.transactions.application.port.in.TransactionIngestionUseCase;
import za.co.capitec.transactiondispute.transactions.application.port.in.TransactionQueryUseCase;
import za.co.capitec.transactiondispute.transactions.application.port.out.TransactionRepositoryPort;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionDisputeServiceApplicationIT extends IntegrationTestBase {

    @Autowired
    ApplicationContext context;

    @Test
    void contextLoads() {
        assertThat(context).isNotNull();
        //security
        assertThat(context.getBean(UserRepositoryPort.class)).isNotNull();
        assertThat(context.getBean(UserIngestionUseCase.class)).isNotNull();
        assertThat(context.getBean(PasswordEncoder.class)).isNotNull();
        assertThat(context.getBean(PasswordService.class)).isNotNull();
        assertThat(context.getBean(TokenRevocationService.class)).isNotNull();
        assertThat(context.getBean(AuthService.class)).isNotNull();
        assertThat(context.getBean(SecurityWebFilterChain.class)).isNotNull();
        assertThat(context.getBean(ReactiveJwtDecoder.class)).isNotNull();
        assertThat(context.getBean(JwtEncoder.class)).isNotNull();
        assertThat(context.getBean(JwtTokenService.class)).isNotNull();
        assertThat(context.getBean(CorsConfigurationSource.class)).isNotNull();

        //disputes
        assertThat(context.getBean(DisputeRepositoryPort.class)).isNotNull();
        assertThat(context.getBean(DisputeHistoryRepositoryPort.class)).isNotNull();
        assertThat(context.getBean(DisputeViewRepositoryPort.class)).isNotNull();
        assertThat(context.getBean(DisputeEventPublisherPort.class)).isNotNull();
        assertThat(context.getBean(TransactionQueryPort.class)).isNotNull();
        assertThat(context.getBean(CreateDisputeUseCase.class)).isNotNull();

        //flyway
        assertThat(context.getBean(ApplicationRunner.class)).isNotNull();

        //transactions
        assertThat(context.getBean(TransactionRepositoryPort.class)).isNotNull();
        assertThat(context.getBean(TransactionQueryUseCase.class)).isNotNull();
        assertThat(context.getBean(TransactionIngestionUseCase.class)).isNotNull();

    }
}
