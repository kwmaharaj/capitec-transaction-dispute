package za.co.capitec.transactiondispute.security.domain.model;

import java.util.List;
import java.util.UUID;

/**
 * Minimal user account model for this take-home.
 *
 * Note: Passwords are stored in plain text for now (demo only).
 * We'll replace this with BCrypt + persistence later.
 */
public record UserAccount(
        UUID userId,
        String username,
        String passwordHash,
        List<String> roles
) {
}
