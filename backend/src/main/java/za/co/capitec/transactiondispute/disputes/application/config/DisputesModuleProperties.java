package za.co.capitec.transactiondispute.disputes.application.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Disputes module functional rules.
 */
@ConfigurationProperties(prefix = "modules.disputes")
public record DisputesModuleProperties(
        Rules rules
) {
    public record Rules(int maxAgeDays) {
        public Rules(int maxAgeDays) {
            this.maxAgeDays = (maxAgeDays <= 0) ? 60 : maxAgeDays;
        }
    }

    public DisputesModuleProperties(Rules rules) {
        this.rules = (rules == null) ? new Rules(60) : rules;
    }
}