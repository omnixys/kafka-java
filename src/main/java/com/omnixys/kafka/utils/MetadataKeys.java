package com.omnixys.kafka.utils;

public final class MetadataKeys {
    public static final String SERVICE = "service";
    public static final String VERSION = "version";
    public static final String METHOD = "method";
    public static final String CLAZZ = "clazz";

    public static final String TRACE_ID = "traceId";
    public static final String SPAN_ID = "spanId";
    public static final String PARENT_SPAN_ID = "parentSpanId";
    public static final String SAMPLED = "sampled";

    private MetadataKeys() {}
}