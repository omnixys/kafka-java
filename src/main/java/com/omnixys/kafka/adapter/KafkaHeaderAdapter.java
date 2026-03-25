package com.omnixys.kafka.adapter;

import com.omnixys.observability.api.HeaderSetter;
import org.apache.kafka.common.header.Headers;

public class KafkaHeaderAdapter implements HeaderSetter {

    private final Headers headers;

    public KafkaHeaderAdapter(Headers headers) {
        this.headers = headers;
    }

    @Override
    public void set(String key, String value) {
        headers.remove(key);
        headers.add(key, value.getBytes());
    }
}