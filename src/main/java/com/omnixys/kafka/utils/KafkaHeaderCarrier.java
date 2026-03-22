package com.omnixys.kafka.utils;

import com.omnixys.observability.propagation.HeaderCarrier;
import org.apache.kafka.common.header.Headers;

import java.nio.charset.StandardCharsets;

/**
 * Kafka adapter for telemetry-core HeaderCarrier abstraction.
 */
public class KafkaHeaderCarrier implements HeaderCarrier {

    private final Headers headers;

    public KafkaHeaderCarrier(Headers headers) {
        this.headers = headers;
    }

    @Override
    public void set(String key, String value) {
        headers.add(key, value.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public String get(String key) {
        var header = headers.lastHeader(key);
        return header != null
                ? new String(header.value(), StandardCharsets.UTF_8)
                : null;
    }
}