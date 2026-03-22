package com.omnixys.kafka.dispatcher;

import com.omnixys.kafka.model.KafkaEnvelope;

public interface KafkaEventHandler<T> {

    void handle(KafkaEnvelope<T> envelope);

}