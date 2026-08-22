package com.omnixys.kafka.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HeaderDataDTOTest {

    @Test
    void of_shouldCreateDtoWithGivenValues() {
        var dto = HeaderDataDTO.of("svc", "1.2", "POST", "OrderHandler");

        assertThat(dto.service()).isEqualTo("svc");
        assertThat(dto.version()).isEqualTo("1.2");
        assertThat(dto.method()).isEqualTo("POST");
        assertThat(dto.clazz()).isEqualTo("OrderHandler");
    }

    @Test
    void empty_shouldReturnAllNullFields() {
        assertThat(HeaderDataDTO.empty())
                .isEqualTo(new HeaderDataDTO(null, null, null, null));
    }

    @Test
    void withers_shouldBeImmutableAndReturnNewInstances() {
        var dto = HeaderDataDTO.empty()
                .withService("svc")
                .withVersion("1.0")
                .withMethod("GET")
                .withClazz("Repo");

        assertThat(dto.service()).isEqualTo("svc");
        assertThat(dto.version()).isEqualTo("1.0");
        assertThat(dto.method()).isEqualTo("GET");
        assertThat(dto.clazz()).isEqualTo("Repo");

        assertThat(HeaderDataDTO.empty().service()).isNull();
        assertThat(HeaderDataDTO.empty().version()).isNull();
    }

    @Test
    void withers_shouldPreserveUnchangedFields() {
        var dto = HeaderDataDTO.of("svc", "1.0", "GET", "Cls").withMethod("POST");

        assertThat(dto.method()).isEqualTo("POST");
        assertThat(dto.service()).isEqualTo("svc");
        assertThat(dto.version()).isEqualTo("1.0");
        assertThat(dto.clazz()).isEqualTo("Cls");
    }
}
