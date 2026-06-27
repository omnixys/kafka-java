package com.omnixys.kafka;

import com.omnixys.kafka.model.EventType;
import com.omnixys.kafka.model.KafkaHeaderMapper;
import com.omnixys.kafka.model.KafkaMetaData;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class KafkaHeaderMapperTest {

    @Test
    void addMetadata_shouldAddAllFieldsAsXMetaHeaders() {
        KafkaMetaData metadata = new KafkaMetaData(
                "my-service", "2.0.0", "MyClass", "doWork", EventType.EVENT
        );
        Headers headers = new RecordHeaders();

        KafkaHeaderMapper.addMetadata(headers, metadata);

        assertThat(headers).hasSize(5);
        assertThat(headerValue(headers, "x-meta-service")).isEqualTo("my-service");
        assertThat(headerValue(headers, "x-meta-version")).isEqualTo("2.0.0");
        assertThat(headerValue(headers, "x-meta-class")).isEqualTo("MyClass");
        assertThat(headerValue(headers, "x-meta-operation")).isEqualTo("doWork");
        assertThat(headerValue(headers, "x-meta-type")).isEqualTo("EVENT");
    }

    @Test
    void addMetadata_shouldUseDefaultsForNullFields() {
        KafkaMetaData metadata = new KafkaMetaData(
                null, null, null, null, EventType.LOG
        );
        Headers headers = new RecordHeaders();

        KafkaHeaderMapper.addMetadata(headers, metadata);

        assertThat(headers).hasSize(5);
        assertThat(headerValue(headers, "x-meta-service")).isEqualTo("unknown-service");
        assertThat(headerValue(headers, "x-meta-version")).isEqualTo("1");
        assertThat(headerValue(headers, "x-meta-class")).isEqualTo("unknown-class");
        assertThat(headerValue(headers, "x-meta-operation")).isEqualTo("unknown-operation");
        assertThat(headerValue(headers, "x-meta-type")).isEqualTo("LOG");
    }

    @Test
    void addMetadata_shouldSkipBlankFields() {
        KafkaMetaData metadata = new KafkaMetaData(
                "  ", " ", "", "", EventType.METRIC
        );
        Headers headers = new RecordHeaders();

        KafkaHeaderMapper.addMetadata(headers, metadata);

        assertThat(headers).hasSize(1);
        assertThat(headerValue(headers, "x-meta-type")).isEqualTo("METRIC");
    }

    @Test
    void addMetadata_shouldDoNothingForNullMetadata() {
        Headers headers = new RecordHeaders();

        KafkaHeaderMapper.addMetadata(headers, null);

        assertThat(headers).isEmpty();
    }

    private static String headerValue(Headers headers, String key) {
        org.apache.kafka.common.header.Header header = headers.lastHeader(key);
        if (header == null) return null;
        return new String(header.value(), StandardCharsets.UTF_8);
    }
}
