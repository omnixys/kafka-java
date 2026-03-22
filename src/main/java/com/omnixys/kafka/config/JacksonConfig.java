package com.omnixys.kafka.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Jackson configuration for the Kafka infrastructure package.
 *
 * <p>
 * Provides a centrally configured {@link ObjectMapper} instance
 * used for serializing and deserializing Kafka message envelopes.
 * </p>
 *
 * <p>
 * The configuration enables automatic module discovery via
 * {@link JsonMapper#builder()}, ensuring support for:
 * </p>
 *
 * <ul>
 *   <li>Java Time (JSR-310)</li>
 *   <li>Parameter names module</li>
 *   <li>Record support</li>
 * </ul>
 *
 * <p>
 * This configuration is designed to be lightweight and reusable
 * across all services importing this Kafka package.
 * </p>
 */
@Configuration
public class JacksonConfig {

    /**
     * Creates a configured {@link ObjectMapper} instance.
     *
     * @return configured ObjectMapper with auto-discovered modules
     */
    @Bean
    public ObjectMapper objectMapper() {
        return JsonMapper.builder()
                .findAndAddModules()
                .build();
    }
}