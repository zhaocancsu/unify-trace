package cn.migu.trace.context;

import org.apache.commons.lang3.StringUtils;

public class Tracer
{
    private final CurrentTraceContext currentTraceContext;
    
    public Tracer(Tracing.Builder builder)
    {
        this.currentTraceContext = builder.currentTraceContext;
    }
    
    public Tracer addTraceContext(TraceContext traceCtx)
    {
        currentTraceContext.newScope(traceCtx);
        return this;
    }
    
    public CurrentTraceContext getCurrentTraceContext()
    {
        return currentTraceContext;
    }
    
    public Span newSpan(boolean isRoot)
    {
        TraceContext traceCtx = currentTraceContext.get();
        if (null == traceCtx)
        {
            throw new NullPointerException("TraceContext Is Null");
        }
        if (isRoot)
        {
            String traceId = String.valueOf(Platform.get().randomLong());
            traceCtx.setTraceId(traceId);
            Span span = Span.create(traceId,
                traceCtx.getTraceName(),
                String.valueOf(Platform.get().randomLong()),
                traceCtx.getSpanName(),
                "",
                traceCtx.getType(),
                Platform.get().hostAddr(),
                String.valueOf(Platform.get().currentTimeMicroseconds()),
                traceCtx.getAnnotation());
            return span;
        }
        else
        {
            String traceId = traceCtx.getTraceId();
            if (StringUtils.isEmpty(traceId))
            {
                throw new NullPointerException("TraceId Is Null In TraceContext");
            }
            
            String parentSpanId = traceCtx.getSpanId();
            Span span = Span.create(traceId,
                traceCtx.getTraceName(),
                String.valueOf(Platform.get().randomLong()),
                traceCtx.getSpanName(),
                parentSpanId,
                traceCtx.getType(),
                Platform.get().hostAddr(),
                String.valueOf(Platform.get().currentTimeMicroseconds()),
                traceCtx.getAnnotation());
            return span;
            
        }
    }
}
