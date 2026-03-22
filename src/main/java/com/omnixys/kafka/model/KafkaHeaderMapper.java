package com.omnixys.kafka.model;

import org.apache.kafka.common.header.Headers;

import java.nio.charset.StandardCharsets;
import java.util.Map;

public final class KafkaHeaderMapper {

    private KafkaHeaderMapper() {}

    public static void addMetadata(Headers headers, Map<String, String> metadata) {
        if (metadata == null) return;

        metadata.forEach((key, value) -> {
            if (key != null && value != null) {
                headers.add(key, value.getBytes(StandardCharsets.UTF_8));
            }
        });
    }
}