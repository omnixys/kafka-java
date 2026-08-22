package com.omnixys.kafka.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HeaderDataMapperTest {

    @Test
    void toMetadata_shouldMapAllNonNullFields() {
        var metadata = HeaderDataMapper.toMetadata(
                new HeaderDataDTO("svc", "1.0", "POST", "OrderHandler"));

        assertThat(metadata)
                .containsEntry("x-meta-service", "svc")
                .containsEntry("x-meta-version", "1.0")
                .containsEntry("x-meta-method", "POST")
                .containsEntry("x-meta-class", "OrderHandler");
    }

    @Test
    void toMetadata_shouldSkipNullFields() {
        var metadata = HeaderDataMapper.toMetadata(HeaderDataDTO.empty());

        assertThat(metadata).isEmpty();
    }

    @Test
    void toMetadata_shouldReturnEmptyMapForNullHeader() {
        assertThat(HeaderDataMapper.toMetadata(null)).isEmpty();
    }

    @Test
    void toMetadata_shouldSkipOnlyNullFields() {
        var metadata = HeaderDataMapper.toMetadata(
                new HeaderDataDTO(null, "1.0", null, "Cls"));

        assertThat(metadata)
                .hasSize(2)
                .containsEntry("x-meta-version", "1.0")
                .containsEntry("x-meta-class", "Cls");
    }
}
