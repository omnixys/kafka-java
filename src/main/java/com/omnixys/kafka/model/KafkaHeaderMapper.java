package com.omnixys.kafka.model;

import org.apache.kafka.common.header.Headers;

import java.nio.charset.StandardCharsets;

import static com.omnixys.kafka.utils.MetadataKeys.*;

public final class KafkaHeaderMapper {

    private KafkaHeaderMapper() {}

    public static void addMetadata(Headers headers, KafkaMetaData metadata) {
        if (metadata == null) return;

        put(headers, SERVICE, metadata.service());
        put(headers, VERSION, metadata.version());
        put(headers, CLAZZ, metadata.clazz());
        put(headers, OPERATION, metadata.operation());

        // enum → name()
        put(headers, TYPE, metadata.type().name());
    }

    private static void put(Headers headers, String key, String value) {
        if (value == null || value.isBlank()) return;

        headers.add(key, value.getBytes(StandardCharsets.UTF_8));
    }
}