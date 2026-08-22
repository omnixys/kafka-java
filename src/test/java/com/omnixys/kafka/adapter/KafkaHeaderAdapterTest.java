package com.omnixys.kafka.adapter;

import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class KafkaHeaderAdapterTest {

    @Test
    void set_shouldAddHeader() {
        Headers headers = new RecordHeaders();

        new KafkaHeaderAdapter(headers).set("x-trace-id", "trace-1");

        assertThat(new String(headers.lastHeader("x-trace-id").value(), StandardCharsets.UTF_8))
                .isEqualTo("trace-1");
    }

    @Test
    void set_shouldReplaceExistingValue() {
        Headers headers = new RecordHeaders()
                .add("x-trace-id", "old".getBytes(StandardCharsets.UTF_8));

        new KafkaHeaderAdapter(headers).set("x-trace-id", "new");

        assertThat(headers.headers("x-trace-id")).hasSize(1);
        assertThat(new String(headers.lastHeader("x-trace-id").value(), StandardCharsets.UTF_8))
                .isEqualTo("new");
    }
}
