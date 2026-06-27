package com.omnixys.kafka.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Standard Kafka event envelope used across all Omnixys services.
 *
 * @param <T> payload type
 */
public record KafkaEnvelope<T>(
        String eventId,
        String eventName,
        EventType eventType,
        String eventVersion,
        String service,
        Instant timestamp,
        T payload
) {

    /**
     * Factory method with default eventType = "event"
     */
    public static <T> KafkaEnvelope<T> of(
            String eventName,
            String service,
            String version,
            T payload
    ) {
        return of(eventName, EventType.EVENT, service, version, payload);
    }

    /**
     * Full factory method with explicit eventType and metadata
     */
    public static <T> KafkaEnvelope<T> of(
            String eventName,
            EventType eventType,
            String service,
            String version,
            T payload
    ) {

        Objects.requireNonNull(eventName, "eventName must not be null");
        Objects.requireNonNull(eventType, "eventType must not be null");
        Objects.requireNonNull(service, "service must not be null");
        Objects.requireNonNull(version, "version must not be null");

        return new KafkaEnvelope<>(
                UUID.randomUUID().toString(),
                eventName,
                eventType,
                version,
                service,
                Instant.now(),
                payload
        );
    }
}