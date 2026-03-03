package za.co.capitec.transactiondispute.shared.infrastructure.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;

@Configuration
@EnableConfigurationProperties(IdempotencyProperties.class)
public class IdempotencyConfig {

    @Bean
    @ConditionalOnBean(ReactiveStringRedisTemplate.class)
    public IdempotencyWebFilter idempotencyWebFilter(
            ReactiveStringRedisTemplate redis,
            IdempotencyProperties props,
            ObjectMapper objectMapper
    ) {
        return new IdempotencyWebFilter(redis, objectMapper, props);
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
