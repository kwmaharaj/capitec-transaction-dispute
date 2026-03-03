package za.co.capitec.transactiondispute.security.infrastructure.http;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import za.co.capitec.transactiondispute.security.application.JwtTokenService;
import za.co.capitec.transactiondispute.security.application.TokenRevocationService;
import za.co.capitec.transactiondispute.security.application.config.SecurityModuleProperties;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.*;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.core.convert.converter.Converter;

import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
@EnableConfigurationProperties(SecurityModuleProperties.class)
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http,
                                                            ReactiveJwtDecoder jwtDecoder,
                                                            CorsConfigurationSource corsConfigSource) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .cors(cors -> cors.configurationSource(corsConfigSource))
                .authorizeExchange(ex -> ex
                        .pathMatchers("/swagger-ui.html", "/swagger-ui/**","/v3/api-docs/**","/webjars/**").permitAll()
                        .pathMatchers("/actuator/health", "/actuator/health/**", "/actuator/info").permitAll() //not adding role to restrict access
                        .pathMatchers("/internal/**").permitAll() //integration point to get data into the system
                        .pathMatchers("/v1/ingest/transactions").permitAll()
                        .pathMatchers("/auth/login", "/auth/register").permitAll()
                        .pathMatchers("/actuator/**").permitAll()
                        .pathMatchers("/v1/support/**").hasRole("SUPPORT")
                        .pathMatchers("/v1/transactions/**").hasAnyRole("USER", "SUPPORT")
                        .anyExchange().authenticated()
                )
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(jwt -> jwt
                                .jwtDecoder(jwtDecoder)
                                .jwtAuthenticationConverter(jwtAuthConverter())
                        )
                )
                .build();
    }

    @Bean
    public ReactiveJwtDecoder jwtDecoder(SecurityModuleProperties props,
                                        TokenRevocationService revocations) {
        SecretKey key = new SecretKeySpec(
                props.jwt().secret().getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
        );

        ReactiveJwtDecoder delegate = NimbusReactiveJwtDecoder.withSecretKey(key).build();

        // Production-grade: perform revocation checks *during authentication* (before any controller writes).
        // This avoids "response already committed" / ReadOnlyHttpHeaders mutations in late WebFilters.
        return token -> delegate.decode(token)
                .flatMap(jwt -> {
                    String jti = jwt.getId();
                    if (jti == null || jti.isBlank()) {
                        return Mono.just(jwt);
                    }
                    return revocations.isRevoked(jti)
                            .flatMap(revoked -> {
                                if (Boolean.TRUE.equals(revoked)) {
                                    return Mono.error(new BadJwtException("JWT revoked"));
                                }
                                return Mono.just(jwt);
                            });
                });
    }

    @Bean
    public JwtEncoder jwtEncoder(SecurityModuleProperties props) {
        SecretKey key = new SecretKeySpec(
                props.jwt().secret().getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
        );
        return new NimbusJwtEncoder(new ImmutableSecret<>(key));
    }

    @Bean
    public JwtTokenService jwtTokenService(JwtEncoder encoder, SecurityModuleProperties props) {
        return new JwtTokenService(encoder, props.jwt().issuer());
    }

    private Converter<Jwt, Mono<AbstractAuthenticationToken>> jwtAuthConverter() {
        var grantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        grantedAuthoritiesConverter.setAuthoritiesClaimName("roles"); // roles claim
        grantedAuthoritiesConverter.setAuthorityPrefix("ROLE_");      // USER -> ROLE_USER

        var jwtConverter = new JwtAuthenticationConverter();
        jwtConverter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);

        return new ReactiveJwtAuthenticationConverterAdapter(jwtConverter);
    }

    /**
     * CORS required for local development where:
     * the browser app is served from http://localhost:3000 and
     * the API is served from http://localhost:8080
     * for dev, the ui is served from http://localhost:5173.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        var config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.addAllowedOriginPattern("http://localhost:3000");
        config.addAllowedOriginPattern("http://localhost:5173");
        config.addAllowedOriginPattern("http://localhost:8080");
        config.addAllowedMethod(CorsConfiguration.ALL);
        config.addAllowedHeader(CorsConfiguration.ALL);
        config.addExposedHeader("Location");

        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}