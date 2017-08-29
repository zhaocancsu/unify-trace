package cn.migu.trace.context;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class Span
{
    public abstract String getTraceId();
    
    public abstract String getTraceName();
    
    public abstract String getSpanId();
    
    public abstract String getSpanName();
    
    public abstract String getSpanParentId();
    
    public abstract String getType();
    
    public abstract String getHost();
    
    public abstract String getTimestamp();
    
    public abstract String getStatus();
    
    public abstract String getAnnotation();
    
    public static Span create(String traceId, String traceName, String spanId, String spanName, String spanParentId,
        String type, String host, String timestamp, String status, String annotation)
    {
        return new AutoValue_Span(traceId, traceName, spanId, spanName, spanParentId, type, host, timestamp, status,
            annotation);
    }
    
}
