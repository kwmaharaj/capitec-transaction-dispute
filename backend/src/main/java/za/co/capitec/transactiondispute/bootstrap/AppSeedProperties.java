package za.co.capitec.transactiondispute.bootstrap;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.UUID;

/**
 * Dev-only seed inputs.
 *
 * IMPORTANT: Passwords here are seed inputs only. They are hashed before being persisted.
 */
@ConfigurationProperties(prefix = "app.seed")
public record AppSeedProperties(
        boolean enabled,
        List<UserSeed> users
) {

    public record UserSeed(
            UUID userId,
            String username,
            String password,
            List<String> roles
    ) {}
}
