package za.co.capitec.transactiondispute.security.application;

import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

public class JwtTokenService {

    private final JwtEncoder encoder;
    private final String issuer;

    public JwtTokenService(JwtEncoder encoder, String issuer) {
        this.encoder = encoder;
        this.issuer = issuer;
    }

    public String issueAccessToken(UUID userId, List<String> roles) {
        Instant now = Instant.now();

        var normalizedRoles = roles == null ? List.<String>of() : roles.stream()
                .map(this::normalizeRole)
                .toList();

        var claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .id(UUID.randomUUID().toString()) //important for revocation from redis
                .issuedAt(now)
                .expiresAt(now.plus(30, ChronoUnit.MINUTES))
                .subject(userId.toString())
                .claim("roles", normalizedRoles)   // ["USER"] not ["ROLE_USER"]
                .build();

        var headers = JwsHeader.with(() -> "HS256").build();
        return encoder.encode(JwtEncoderParameters.from(headers, claims)).getTokenValue();
    }

    private String normalizeRole(String role) {
        if (role == null) {
            return null;
        }
        if (role.startsWith("ROLE_")) {
            return role.substring("ROLE_".length());
        }
        return role;
    }
}