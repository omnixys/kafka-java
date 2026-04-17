package com.omnixys.kafka.model;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static com.omnixys.kafka.utils.MetadataKeys.*;

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

        // --- Validation ---
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

    /**
     * Attach header data into metadata
     */
    public KafkaEnvelope<T> withHeader(HeaderDataDTO header) {
        // Map<String, String> newMetadata = new HashMap<>(this.metadata);
        // newMetadata.putAll(HeaderDataMapper.toMetadata(header));

        return new KafkaEnvelope<>(
                this.eventId,
                this.eventName,
                this.eventType,
                this.eventVersion,
                this.service,
                this.timestamp,
                this.payload
        );
    }

    public KafkaEnvelope<T> withTrace(final String traceId, final String spanId, final String parentSpanId, final String sampled) {
        // Map<String, String> newMetadata = new HashMap<>(this.metadata);

        // if (traceId != null) newMetadata.put(TRACE_ID, traceId);
        // if (spanId != null) newMetadata.put(SPAN_ID, spanId);
        // if (parentSpanId != null) newMetadata.put(PARENT_SPAN_ID, parentSpanId);
        // if (sampled != null) newMetadata.put(SAMPLED, sampled);

        return new KafkaEnvelope<>(
                this.eventId,
                this.eventName,
                this.eventType,
                this.eventVersion,
                this.service,
                this.timestamp,
                this.payload
        );
    }

    /**
     * Add metadata entry (immutable copy pattern)
     */
    public KafkaEnvelope<T> withMetadata(String key, String value) {
        // Map<String, String> newMetadata = new HashMap<>(this.metadata);
        // newMetadata.put(key, value);

        return new KafkaEnvelope<>(
                this.eventId,
                this.eventName,
                this.eventType,
                this.eventVersion,
                this.service,
                this.timestamp,
                this.payload
        );
    }
}