package com.omnixys.kafka.utils;

public final class MetadataKeys {
    public static final String CLAZZ = "x-meta-clazz";
    public static final String SERVICE = "x-meta-service";
    public static final String METHOD = "x-meta-method";
    public static final String VERSION = "x-meta-version";
    public static final String OPERATION = "x-meta-operation";
    public static final String TYPE = "x-meta-type";

    public static final String TRACE_ID = "x-meta-traceId";
    public static final String SPAN_ID = "x-meta-spanId";
    public static final String PARENT_SPAN_ID = "x-meta-parentSpanId";
    public static final String SAMPLED = "x-meta-sampled";

    public static final String TRACE_PARENT = "traceparent";
    public static final String TRACE_STATE = "tracestate";

    private MetadataKeys() {}
}