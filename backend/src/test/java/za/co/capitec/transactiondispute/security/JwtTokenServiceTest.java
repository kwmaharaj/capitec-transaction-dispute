package za.co.capitec.transactiondispute.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import za.co.capitec.transactiondispute.security.application.JwtTokenService;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenServiceTest {

    @Test
    void issueAccessToken_normalizes_roles_to_non_prefixed_values() {
        String secret = "change-me-please-change-me-please-change-me";
        SecretKey key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");

        var encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
        var svc = new JwtTokenService(encoder, "http://issuer-x");

        UUID userId = UUID.randomUUID();
        String token = svc.issueAccessToken(userId, List.of("ROLE_USER", "SUPPORT"));

        var decoder = NimbusJwtDecoder.withSecretKey(key).build();
        var jwt = decoder.decode(token);

        assertThat(jwt.getSubject()).isEqualTo(userId.toString());
        assertThat(jwt.getIssuer()).hasToString("http://issuer-x");
        assertThat(jwt.getClaimAsStringList("roles")).containsExactly("USER", "SUPPORT");
    }
}
