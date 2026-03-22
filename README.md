# Omnixys Kafka Spring

## Purpose
Reusable Kafka infrastructure for Spring Boot microservices.

## Features
- Producer / Consumer abstraction
- Event dispatcher
- OpenTelemetry propagation
- Structured logging
- Auto configuration
- Gradle publishing

## Installation
```kotlin
implementation("com.omnixys.kafka:omnixys-kafka-spring:1.0.0")
````

## Producer Usage

```java
producer.send("topic",
    KafkaEnvelope.of("event", "service", "v1", "create", payload));
```

## Consumer Usage

```java
@Component
public class UserHandler implements KafkaEventHandler {

    public UserHandler(KafkaEventDispatcher dispatcher) {
        dispatcher.register("user.create", this);
    }

    @Override
    public void handle(KafkaEnvelope<?> envelope) {
        // handle event
    }
}
```

## Tracing

* Uses W3C Trace Context
* Producer injects context
* Consumer extracts context
* TraceId is preserved across services