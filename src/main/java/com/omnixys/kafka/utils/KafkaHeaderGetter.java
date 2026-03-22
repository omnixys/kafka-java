package com.omnixys.kafka.utils;

import io.opentelemetry.context.propagation.TextMapGetter;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Extracts headers from Kafka for OpenTelemetry propagation.
 */
public class KafkaHeaderGetter implements TextMapGetter<Headers> {

    @Override
    public Iterable<String> keys(Headers carrier) {
        List<String> keys = new ArrayList<>();
        carrier.forEach(h -> keys.add(h.key()));
        return keys;
    }

    @Override
    public String get(Headers carrier, String key) {
        if (carrier == null) return null;
        Header header = carrier.lastHeader(key);
        if (header == null) return null;
        return new String(header.value(), StandardCharsets.UTF_8);
    }
}