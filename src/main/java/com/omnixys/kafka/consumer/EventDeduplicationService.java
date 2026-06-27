package com.omnixys.kafka.consumer;

/**
 * Optional service for Kafka consumer idempotency.
 * Implementations should store processed eventIds with a TTL
 * to prevent duplicate event processing.
 */
public interface EventDeduplicationService {

    /**
     * Returns true if this eventId has already been processed.
     */
    boolean isDuplicate(String eventId);

    /**
     * Marks an eventId as processed with the given TTL in seconds.
     */
    void markProcessed(String eventId, long ttlSeconds);
}
