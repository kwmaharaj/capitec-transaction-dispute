package za.co.capitec.transactiondispute.shared.infrastructure.cache;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class JsonCacheCodec {

    private final ObjectMapper objectMapper;

    /**
     * Default constructor (creates its own mapper).
     */
    public JsonCacheCodec() {
        this(new ObjectMapper().findAndRegisterModules());
    }

    /**
     * Keeps your existing wiring: new JsonCacheCodec(om)
     */
    public JsonCacheCodec(ObjectMapper objectMapper) {
        // copy() so we don't mutate the app-wide ObjectMapper bean
        ObjectMapper mapper = objectMapper.copy();
        configure(mapper);
        this.objectMapper = mapper;
    }

    private static void configure(ObjectMapper mapper) {
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE);
        mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false);
    }

    public String write(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize cache value", e);
        }
    }

    public <T> T read(String json, Class<T> targetType) {
        try {
            return objectMapper.readValue(json, targetType);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize cache value", e);
        }
    }

    public <T> T read(String json, TypeReference<T> targetType) {
        try {
            return objectMapper.readValue(json, targetType);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize cache value", e);
        }
    }
}