package com.omnixys.kafka.utils;

import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class KafkaHeaderGetterTest {

    private final KafkaHeaderGetter getter = new KafkaHeaderGetter();

    @Test
    void keys_shouldReturnAllHeaderKeys() {
        Headers headers = new RecordHeaders()
                .add("traceparent", "00-trace".getBytes(StandardCharsets.UTF_8))
                .add("tracestate", "congo=1".getBytes(StandardCharsets.UTF_8));

        assertThat(getter.keys(headers)).containsExactlyInAnyOrder("traceparent", "tracestate");
    }

    @Test
    void get_shouldReturnValueForExistingKey() {
        Headers headers = new RecordHeaders()
                .add("traceparent", "00-abcdef".getBytes(StandardCharsets.UTF_8));

        assertThat(getter.get(headers, "traceparent")).isEqualTo("00-abcdef");
    }

    @Test
    void get_shouldReturnNullForMissingKey() {
        Headers headers = new RecordHeaders()
                .add("traceparent", "00-abcdef".getBytes(StandardCharsets.UTF_8));

        assertThat(getter.get(headers, "missing")).isNull();
    }

    @Test
    void get_shouldReturnNullForNullCarrier() {
        assertThat(getter.get(null, "traceparent")).isNull();
    }

    @Test
    void get_shouldReturnLastHeaderValueWhenRepeated() {
        Headers headers = new RecordHeaders()
                .add("traceparent", "00-first".getBytes(StandardCharsets.UTF_8))
                .add("traceparent", "00-second".getBytes(StandardCharsets.UTF_8));

        assertThat(getter.get(headers, "traceparent")).isEqualTo("00-second");
    }
}
