package com.omnixys.kafka.utils;

import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class KafkaHeaderCarrierTest {

    @Test
    void setAndGet_shouldRoundTripValue() {
        Headers headers = new RecordHeaders();
        var carrier = new KafkaHeaderCarrier(headers);

        carrier.set("x-key", "value-1");

        assertThat(carrier.get("x-key")).isEqualTo("value-1");
    }

    @Test
    void get_shouldReturnNullWhenHeaderAbsent() {
        var carrier = new KafkaHeaderCarrier(new RecordHeaders());

        assertThat(carrier.get("missing")).isNull();
    }

    @Test
    void get_shouldReturnLastHeaderValue() {
        Headers headers = new RecordHeaders()
                .add("x-key", "v1".getBytes(StandardCharsets.UTF_8))
                .add("x-key", "v2".getBytes(StandardCharsets.UTF_8));
        var carrier = new KafkaHeaderCarrier(headers);

        assertThat(carrier.get("x-key")).isEqualTo("v2");
    }

    @Test
    void set_shouldAppendToExistingHeaders() {
        Headers headers = new RecordHeaders();
        var carrier = new KafkaHeaderCarrier(headers);

        carrier.set("x-key", "v1");
        carrier.set("x-key", "v2");

        assertThat(headers.headers("x-key")).hasSize(2);
    }

    @Test
    void get_shouldDecodeUtf8() {
        Headers headers = new RecordHeaders()
                .add("x-key", "über™".getBytes(StandardCharsets.UTF_8));

        assertThat(new KafkaHeaderCarrier(headers).get("x-key")).isEqualTo("über™");
    }
}
