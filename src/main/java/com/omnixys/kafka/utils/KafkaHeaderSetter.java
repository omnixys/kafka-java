package com.omnixys.kafka.utils;

import io.opentelemetry.context.propagation.TextMapSetter;
import org.apache.kafka.common.header.Headers;

import java.nio.charset.StandardCharsets;

/**
 * Injects OpenTelemetry context into Kafka headers.
 */
public class KafkaHeaderSetter implements TextMapSetter<Headers> {

    @Override
    public void set(Headers headers, String key, String value) {
        if (headers == null || key == null || value == null) return;

        headers.remove(key);
        headers.add(key, value.getBytes(StandardCharsets.UTF_8));
    }
}