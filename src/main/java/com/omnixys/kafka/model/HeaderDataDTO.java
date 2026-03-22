package com.omnixys.kafka.model;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Metadata extracted from service context and attached to Kafka events.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record HeaderDataDTO(
        String service,
        String version,
        String method,
        String clazz
) {

    public static HeaderDataDTO of(
            String service,
            String version,
            String method,
            String clazz
    ) {
        return new HeaderDataDTO(service, version, method, clazz);
    }

    public static HeaderDataDTO empty() {
        return new HeaderDataDTO(null, null, null, null);
    }

    public HeaderDataDTO withService(String service) {
        return new HeaderDataDTO(service, this.version, this.method, this.clazz);
    }

    public HeaderDataDTO withVersion(String version) {
        return new HeaderDataDTO(this.service, version, this.method, this.clazz);
    }

    public HeaderDataDTO withMethod(String method) {
        return new HeaderDataDTO(this.service, this.version, method, this.clazz);
    }

    public HeaderDataDTO withClazz(String clazz) {
        return new HeaderDataDTO(this.service, this.version, this.method, clazz);
    }
}