package com.omnixys.kafka.consumer;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

class EventDeduplicationServiceTest {

    private final EventDeduplicationService dedup = new EventDeduplicationService() {
        private final Set<String> processed = ConcurrentHashMap.newKeySet();

        @Override
        public boolean isDuplicate(String eventId) {
            return eventId != null && processed.contains(eventId);
        }

        @Override
        public void markProcessed(String eventId, long ttlSeconds) {
            if (eventId != null) processed.add(eventId);
        }
    };

    @Test
    void shouldNotBeDuplicateBeforeMarking() {
        assertFalse(dedup.isDuplicate("event-1"));
    }

    @Test
    void shouldBeDuplicateAfterMarking() {
        dedup.markProcessed("event-1", 3600);
        assertTrue(dedup.isDuplicate("event-1"));
    }

    @Test
    void shouldHandleMultipleEvents() {
        dedup.markProcessed("event-1", 3600);
        dedup.markProcessed("event-2", 3600);

        assertTrue(dedup.isDuplicate("event-1"));
        assertTrue(dedup.isDuplicate("event-2"));
        assertFalse(dedup.isDuplicate("event-3"));
    }

    @Test
    void shouldHandleEmptyEventId() {
        dedup.markProcessed("", 3600);
        assertTrue(dedup.isDuplicate(""));
    }

    @Test
    void shouldHandleNullEventId() {
        assertFalse(dedup.isDuplicate(null));
    }
}
