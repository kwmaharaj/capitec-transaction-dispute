package za.co.capitec.transactiondispute.bootstrap;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import za.co.capitec.transactiondispute.security.application.*;
import za.co.capitec.transactiondispute.security.application.port.in.UserIngestionUseCase;
import za.co.capitec.transactiondispute.security.application.port.out.UserRepositoryPort;
import za.co.capitec.transactiondispute.security.infrastructure.persistence.SpringDataTemplateUserRepository;


@Configuration
public class SecurityModuleConfig {

    @Bean
    public UserRepositoryPort userRepositoryPort(@Qualifier("securityTemplate") R2dbcEntityTemplate securityTemplate) {
        return new SpringDataTemplateUserRepository(securityTemplate);
    }

    @Bean
    public UserIngestionUseCase userIngestionUseCase(UserRepositoryPort repo) {
        return new UserIngestionService(repo);
    }

    /*
     forcing argon, default is brcypt
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
    }

    @Bean
    public PasswordService passwordService(PasswordEncoder encoder) {
        return new PasswordService(encoder);
    }

    @Bean
    public TokenRevocationService tokenRevocationService(ReactiveStringRedisTemplate redis) {
        return new TokenRevocationService(redis);
    }

    @Bean
    public AuthService authService(UserRepositoryPort users, PasswordService passwords, JwtTokenService tokens, TokenRevocationService revocations) {
        return new AuthService(users, passwords, tokens, revocations);
    }
}
