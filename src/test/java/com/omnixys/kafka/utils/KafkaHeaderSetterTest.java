package com.omnixys.kafka.utils;

import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class KafkaHeaderSetterTest {

    private final KafkaHeaderSetter setter = new KafkaHeaderSetter();

    @Test
    void set_shouldAddHeader() {
        Headers headers = new RecordHeaders();

        setter.set(headers, "traceparent", "00-abcdef");

        assertThat(new String(headers.lastHeader("traceparent").value(), StandardCharsets.UTF_8))
                .isEqualTo("00-abcdef");
    }

    @Test
    void set_shouldReplaceExistingValue() {
        Headers headers = new RecordHeaders()
                .add("traceparent", "00-old".getBytes(StandardCharsets.UTF_8));

        setter.set(headers, "traceparent", "00-new");

        assertThat(headers.headers("traceparent")).hasSize(1);
        assertThat(new String(headers.lastHeader("traceparent").value(), StandardCharsets.UTF_8))
                .isEqualTo("00-new");
    }

    @Test
    void set_shouldIgnoreNullArguments() {
        Headers headers = new RecordHeaders();

        setter.set(null, "k", "v");
        setter.set(headers, null, "v");
        setter.set(headers, "k", null);

        assertThat(headers).isEmpty();
    }
}
