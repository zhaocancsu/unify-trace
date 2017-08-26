package cn.migu.trace.context;

public interface PropagationKeys
{
    String TRACE_ID = "X-Migu-TraceId";
    
    String TRACE_NAME = "X-Migu-TraceName";
    
    String SPAN_ID = "X-Migu-SpanId";
    
    String TRACER_KEY = "Migu-Tracer-Key";
}
