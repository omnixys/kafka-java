package com.omnixys.kafka.model;

import java.util.HashMap;
import java.util.Map;

import static com.omnixys.kafka.utils.MetadataKeys.*;

/**
 * Converts HeaderData into Kafka metadata map.
 */
public final class HeaderDataMapper {

    private HeaderDataMapper() {}

    public static Map<String, String> toMetadata(HeaderDataDTO header) {
        Map<String, String> map = new HashMap<>();

        if (header == null) {
            return map;
        }

        if (header.service() != null) map.put(SERVICE, header.service());
        if (header.version() != null) map.put(VERSION , header.version());
        if (header.method() != null) map.put(METHOD, header.method());
        if (header.clazz() != null) map.put(CLAZZ, header.clazz());

        return map;
    }
}

