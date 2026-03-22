package com.omnixys.kafka.utils;

import org.apache.kafka.common.header.Headers;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public final class KafkaHeaderUtils {

    private KafkaHeaderUtils() {}

    public static Map<String, String> toMap(Headers headers) {
        Map<String, String> map = new HashMap<>();

        headers.forEach(header -> {
            if (header.key() != null && header.value() != null) {
                map.put(
                        header.key(),
                        new String(header.value(), StandardCharsets.UTF_8)
                );
            }
        });

        return map;
    }
}