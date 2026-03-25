package com.omnixys.kafka.model;

public record KafkaMetaData(
        String service,
        String version,
        String clazz,
        String operation,
        EventType type
) {
    public KafkaMetaData {
        if (type == null) {
            throw new IllegalArgumentException("KafkaMetaData.type must not be null");
        }

        service = service != null ? service : "unknown-service";
        version = version != null ? version : "1";
        clazz = clazz != null ? clazz : "unknown-class";
        operation = operation != null ? version : "unknown-operation";
    }
}