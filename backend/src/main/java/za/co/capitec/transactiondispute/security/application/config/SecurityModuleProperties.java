package za.co.capitec.transactiondispute.security.application.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "modules.security")
public record SecurityModuleProperties(
        Jwt jwt
) {
    public record Jwt(String issuer, String secret) {}
}