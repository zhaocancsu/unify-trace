package cn.migu.trace.instrument;

import java.util.List;

import org.apache.http.Header;

import cn.migu.trace.context.Tracer;

public interface IClientInteceptor
{
    void preHandler(Tracer tracer, List<Header> headers, String traceName);
    
    void afterHandler(Tracer tracer, String anno);
    
    void afterExcepHandler(Tracer tracer, Throwable e);
}
