package za.co.capitec.transactiondispute.bootstrap;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import za.co.capitec.transactiondispute.shared.infrastructure.cache.*;

@Configuration
@EnableConfigurationProperties(CacheProperties.class)
public class CacheConfig {

    @Bean
    public ReactiveCacheStore reactiveCacheStore(ReactiveStringRedisTemplate redis) {
        return new RedisReactiveCacheStore(redis);
    }

    @Bean
    public JsonCacheCodec jsonCacheCodec(ObjectMapper objectMapper) {
        return new JsonCacheCodec(objectMapper);
    }

    @Bean
    public CacheKeyFactory cacheKeyFactory(ReactiveCacheStore store, ObjectMapper objectMapper) {
        return new CacheKeyFactory(store, objectMapper);
    }

    @Bean
    public ReactiveCacheAside reactiveCacheAside(ReactiveCacheStore store, JsonCacheCodec codec, CacheProperties props) {
        return new ReactiveCacheAside(store, codec, props);
    }
}
