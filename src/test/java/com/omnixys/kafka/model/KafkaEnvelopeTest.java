package com.omnixys.kafka.model;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KafkaEnvelopeTest {

    @Test
    void of_shouldCreateEnvelopeWithEventTypeDefault() {
        var envelope = KafkaEnvelope.of("order.created", "order-service", "1.0", Map.of("id", "1"));

        assertThat(envelope.eventName()).isEqualTo("order.created");
        assertThat(envelope.eventType()).isEqualTo(EventType.EVENT);
        assertThat(envelope.service()).isEqualTo("order-service");
        assertThat(envelope.eventVersion()).isEqualTo("1.0");
        assertThat(envelope.eventId()).isNotBlank();
        assertThat(envelope.timestamp()).isNotNull();
        assertThat(envelope.payload()).containsEntry("id", "1");
    }

    @Test
    void of_shouldUseExplicitEventType() {
        var envelope = KafkaEnvelope.of("user.login", EventType.LOG, "auth-service", "2.0", "data");

        assertThat(envelope.eventType()).isEqualTo(EventType.LOG);
        assertThat(envelope.payload()).isEqualTo("data");
    }

    @Test
    void of_shouldGenerateUniqueEventIds() {
        var first = KafkaEnvelope.of("order.created", "svc", "1", "p");
        var second = KafkaEnvelope.of("order.created", "svc", "1", "p");

        assertThat(first.eventId()).isNotEqualTo(second.eventId());
    }

    @Test
    void of_shouldRejectNullArguments() {
        assertThatThrownBy(() -> KafkaEnvelope.of(null, "svc", "1", "p"))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("eventName");
        assertThatThrownBy(() -> KafkaEnvelope.of("n", (EventType) null, "svc", "1", "p"))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("eventType");
        assertThatThrownBy(() -> KafkaEnvelope.of("n", EventType.EVENT, null, "1", "p"))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("service");
        assertThatThrownBy(() -> KafkaEnvelope.of("n", EventType.EVENT, "svc", null, "p"))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("version");
    }

    @Test
    void shouldExposeAllRecordAccessors() {
        var timestamp = java.time.Instant.parse("2025-01-01T00:00:00Z");
        var envelope = new KafkaEnvelope<>(
                "evt-1", "order.created", EventType.EVENT, "1.0", "svc", timestamp, "payload");

        assertThat(envelope.eventId()).isEqualTo("evt-1");
        assertThat(envelope.eventName()).isEqualTo("order.created");
        assertThat(envelope.eventType()).isEqualTo(EventType.EVENT);
        assertThat(envelope.eventVersion()).isEqualTo("1.0");
        assertThat(envelope.service()).isEqualTo("svc");
        assertThat(envelope.timestamp()).isEqualTo(timestamp);
        assertThat(envelope.payload()).isEqualTo("payload");
    }
}
