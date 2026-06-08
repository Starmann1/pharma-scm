package pharma.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public final class JsonConfig {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    private JsonConfig() {
    }

    public static ObjectMapper objectMapper() {
        return OBJECT_MAPPER;
    }
}
