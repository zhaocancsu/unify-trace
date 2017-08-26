package cn.migu.trace.context;

public class Span
{
    private String traceId;
    
    private String traceName;
    
    private String spanId;
    
    private String spanName;
    
    private String spanParentId;
    
    private String type;
    
    private String host;
    
    private String timestamp;
    
    private String annotation;
    
    public Span()
    {
        
    }
    
    public Span(String traceId, String traceName, String spanId, String spanName, String spanParentId, String type,
        String host, String timestamp, String annotation)
    {
        super();
        this.traceId = traceId;
        this.traceName = traceName;
        this.spanId = spanId;
        this.spanName = spanName;
        this.spanParentId = spanParentId;
        this.type = type;
        this.host = host;
        this.timestamp = timestamp;
        this.annotation = annotation;
    }
    
    public String getTraceId()
    {
        return traceId;
    }
    
    public void setTraceId(String traceId)
    {
        this.traceId = traceId;
    }
    
    public String getTraceName()
    {
        return traceName;
    }
    
    public void setTraceName(String traceName)
    {
        this.traceName = traceName;
    }
    
    public String getSpanId()
    {
        return spanId;
    }
    
    public void setSpanId(String spanId)
    {
        this.spanId = spanId;
    }
    
    public String getSpanName()
    {
        return spanName;
    }
    
    public void setSpanName(String spanName)
    {
        this.spanName = spanName;
    }
    
    public String getSpanParentId()
    {
        return spanParentId;
    }
    
    public void setSpanParentId(String spanParentId)
    {
        this.spanParentId = spanParentId;
    }
    
    public String getType()
    {
        return type;
    }
    
    public void setType(String type)
    {
        this.type = type;
    }
    
    public String getHost()
    {
        return host;
    }
    
    public void setHost(String host)
    {
        this.host = host;
    }
    
    public String getTimestamp()
    {
        return timestamp;
    }
    
    public void setTimestamp(String timestamp)
    {
        this.timestamp = timestamp;
    }
    
    public String getAnnotation()
    {
        return annotation;
    }
    
    public void setAnnotation(String annotation)
    {
        this.annotation = annotation;
    }
    
    @Override
    public String toString()
    {
        return "Span [traceId=" + traceId + ", traceName=" + traceName + ", spanId=" + spanId + ", spanName="
            + spanName + ", spanParentId=" + spanParentId + ", type=" + type + ", host=" + host + ", timestamp="
            + timestamp + ", annotation=" + annotation + "]";
    }
    
}
