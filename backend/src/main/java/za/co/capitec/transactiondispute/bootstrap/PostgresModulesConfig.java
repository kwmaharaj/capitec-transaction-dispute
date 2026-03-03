package za.co.capitec.transactiondispute.bootstrap;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(PostgresModulesProperties.class) // <----
public class PostgresModulesConfig {

}