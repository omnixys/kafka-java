package com.omnixys.kafka.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KafkaMetaDataTest {

    @Test
    void shouldExposeAllAccessors() {
        var metadata = new KafkaMetaData("svc", "2.0.0", "MyClass", "doWork", EventType.COMMAND);

        assertThat(metadata.service()).isEqualTo("svc");
        assertThat(metadata.version()).isEqualTo("2.0.0");
        assertThat(metadata.clazz()).isEqualTo("MyClass");
        assertThat(metadata.operation()).isEqualTo("doWork");
        assertThat(metadata.type()).isEqualTo(EventType.COMMAND);
    }

    @Test
    void shouldDefaultNullFields() {
        var metadata = new KafkaMetaData(null, null, null, null, EventType.ALERT);

        assertThat(metadata.service()).isEqualTo("unknown-service");
        assertThat(metadata.version()).isEqualTo("1");
        assertThat(metadata.clazz()).isEqualTo("unknown-class");
        assertThat(metadata.operation()).isEqualTo("unknown-operation");
        assertThat(metadata.type()).isEqualTo(EventType.ALERT);
    }

    @Test
    void shouldRejectNullType() {
        assertThatThrownBy(() -> new KafkaMetaData("svc", "1", "Cls", "op", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("type");
    }
}
