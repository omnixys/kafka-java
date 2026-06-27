package com.omnixys.kafka;

import com.omnixys.kafka.utils.KafkaHeaderUtils;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

class KafkaHeaderUtilsTest {

    @Test
    void toMap_shouldConvertHeadersToMap() {
        Headers headers = new RecordHeaders()
                .add("key1", "value1".getBytes(StandardCharsets.UTF_8))
                .add("key2", "value2".getBytes(StandardCharsets.UTF_8));

        Map<String, String> result = KafkaHeaderUtils.toMap(headers);

        assertThat(result).hasSize(2)
                .contains(entry("key1", "value1"), entry("key2", "value2"));
    }

    @Test
    void toMap_shouldReturnEmptyMapForEmptyHeaders() {
        Headers headers = new RecordHeaders();

        Map<String, String> result = KafkaHeaderUtils.toMap(headers);

        assertThat(result).isEmpty();
    }

    @Test
    void toMap_shouldSkipHeadersWithNullKey() {
        Headers headers = new RecordHeaders()
                .add("key1", "value1".getBytes(StandardCharsets.UTF_8))
                .add(new Header() {
                    @Override
                    public String key() {
                        return null;
                    }

                    @Override
                    public byte[] value() {
                        return "value2".getBytes(StandardCharsets.UTF_8);
                    }
                });

        Map<String, String> result = KafkaHeaderUtils.toMap(headers);

        assertThat(result).hasSize(1).containsKey("key1");
    }

    @Test
    void toMap_shouldSkipHeadersWithNullValue() {
        Headers headers = new RecordHeaders()
                .add("key1", "value1".getBytes(StandardCharsets.UTF_8))
                .add(new Header() {
                    @Override
                    public String key() {
                        return "key2";
                    }

                    @Override
                    public byte[] value() {
                        return null;
                    }
                });

        Map<String, String> result = KafkaHeaderUtils.toMap(headers);

        assertThat(result).hasSize(1).containsKey("key1");
    }

    @Test
    void toMap_shouldDecodeBytesAsUtf8() {
        String unicode = "über-cool™";
        Headers headers = new RecordHeaders()
                .add("key", unicode.getBytes(StandardCharsets.UTF_8));

        Map<String, String> result = KafkaHeaderUtils.toMap(headers);

        assertThat(result).contains(entry("key", unicode));
    }
}
