package cn.migu.trace.context;

/**
 * tracing context
 * 
 * @author  zhaocan
 * @version  [版本号, 2017年8月24日]
 * @see  [相关类/方法]
 * @since  [产品/模块版本]
 */
public class TraceContext implements Cloneable
{
    private final long threadId;
    
    //调用链id
    private String traceId;
    
    //调用链名称
    private String traceName = "";
    
    //
    private String inheritedSpanId = "";
    
    //
    private String localParentSpanId = "";
    
    //
    private String cacheSpanName;
    
    //请求id
    private String spanId = "";
    
    //请求别名
    private String spanName;
    
    //请求类型
    private String type;
    
    //主机地址
    private String host;
    
    //额外注释
    private String annotation = "";
    
    public TraceContext()
    {
        this.threadId = Thread.currentThread().getId();
    }
    
    /*public TraceContext(String traceId, String traceName, String spanId, String spanName, String type, String host,
        String annotation)
    {
        this.traceId = traceId;
        this.traceName = traceName;
        this.spanId = spanId;
        this.spanName = spanName;
        this.type = type;
        this.host = host;
        this.annotation = annotation;
    }*/
    
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
    
    public String getAnnotation()
    {
        return annotation;
    }
    
    public void setAnnotation(String annotation)
    {
        this.annotation = annotation;
    }
    
    public String getCacheSpanName()
    {
        return cacheSpanName;
    }
    
    public void setCacheSpanName(String cacheSpanName)
    {
        this.cacheSpanName = cacheSpanName;
    }
    
    public String getInheritedSpanId()
    {
        return inheritedSpanId;
    }
    
    public void setInheritedSpanId(String inheritedSpanId)
    {
        this.inheritedSpanId = inheritedSpanId;
    }
    
    public String getLocalParentSpanId()
    {
        return localParentSpanId;
    }
    
    public void setLocalParentSpanId(String localParentSpanId)
    {
        this.localParentSpanId = localParentSpanId;
    }
    
    public long getThreadId()
    {
        return threadId;
    }
    
    @Override
    public Object clone()
        throws CloneNotSupportedException
    {
        return super.clone();
    }
    
    @Override
    public String toString()
    {
        return "TraceContext [traceId=" + traceId + ", traceName=" + traceName + ", spanId=" + spanId + ", spanName="
            + spanName + ", type=" + type + ", host=" + host + ", annotation=" + annotation + "]";
    }
    
}
