package cn.migu.trace.instrument;

import java.util.List;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.message.BasicHeader;

import cn.migu.trace.context.PropagationKeys;
import cn.migu.trace.context.ReportRequestType;
import cn.migu.trace.context.Span;
import cn.migu.trace.context.TraceContext;
import cn.migu.trace.context.Tracer;
import cn.migu.trace.internal.Util;

public class HttpClientInteceptor implements IHttpClientInteceptor
{
    private String serviceName;
    
    public HttpClientInteceptor(String serviceName)
    {
        this.serviceName = serviceName;
    }
    
    @Override
    public void preHandler(Tracer tracer, List<Header> headers, String traceName)
    {
        TraceContext traceCtx = tracer.getCurrentTraceContext().get();
        if (null != traceCtx)
        {
            String traceId = traceCtx.getTraceId();
            if (StringUtils.isEmpty(traceId))
            {
                throw new NullPointerException("Trace Id Is Null");
            }
            traceCtx.setType(ReportRequestType.CS);
            
            //module name + top method
            String spanName = StringUtils
                .join(serviceName, ":", Util.topMethodStack(Thread.currentThread().getStackTrace()), "->http client");
            if (!StringUtils.isEmpty(traceCtx.getSpanName()))
            {
                traceCtx.setCacheSpanName(traceCtx.getSpanName());
            }
            traceCtx.setSpanName(spanName);
            
            if (StringUtils.isNotEmpty(traceCtx.getSpanId()))
            {
                traceCtx.setLocalParentSpanId(traceCtx.getSpanId());
            }
            
            Span span = tracer.newSpan(false, "0", false);
            //System.out.println(span);
            SenderTool.sendSpan(tracer.getReporter(), span);
            traceCtx.setSpanId(span.getSpanId());
            
            headers.add(new BasicHeader(PropagationKeys.TRACE_ID, traceId));
            headers.add(new BasicHeader(PropagationKeys.SPAN_ID, span.getSpanId()));
            if (StringUtils.isNotEmpty(traceCtx.getTraceName()))
            {
                headers.add(new BasicHeader(PropagationKeys.TRACE_NAME, traceCtx.getTraceName()));
            }
            
        }
        else
        {
            TraceContext ctx = new TraceContext();
            if (StringUtils.isNotEmpty(serviceName))
            {
                ctx.setSpanName(serviceName);
            }
            if (StringUtils.isNotEmpty(traceName))
            {
                ctx.setTraceName(traceName);
            }
            String spanName = StringUtils
                .join(serviceName, ":", Util.topMethodStack(Thread.currentThread().getStackTrace()), "->httpclient");
            ctx.setSpanName(spanName);
            ctx.setType(ReportRequestType.CS);
            tracer.addTraceContext(ctx);
            Span span = tracer.newSpan(true, "0", false);
            ctx.setSpanId(span.getSpanId());
            //System.out.println(span);
            SenderTool.sendSpan(tracer.getReporter(), span);
            
            headers.add(new BasicHeader(PropagationKeys.TRACE_ID, span.getTraceId()));
            headers.add(new BasicHeader(PropagationKeys.SPAN_ID, span.getSpanId()));
            if (StringUtils.isNotEmpty(span.getTraceName()))
            {
                headers.add(new BasicHeader(PropagationKeys.TRACE_NAME, span.getTraceName()));
            }
        }
        
    }
    
    @Override
    public void afterHandler(Tracer tracer)
    {
        TraceContext traceCtx = tracer.getCurrentTraceContext().get();
        if (null != traceCtx)
        {
            String traceId = traceCtx.getTraceId();
            if (StringUtils.isEmpty(traceId))
            {
                throw new NullPointerException("Trace Id Is Null");
            }
            traceCtx.setType(ReportRequestType.CR);
            
            //module name + top method
            Span span = tracer.newSpan(false, "0", true);
            //System.out.println(span);
            SenderTool.sendSpan(tracer.getReporter(), span);
            
            traceCtx.setAnnotation("");
            if (StringUtils.isNotEmpty(traceCtx.getLocalParentSpanId()))
            {
                traceCtx.setSpanId(traceCtx.getLocalParentSpanId());
                traceCtx.setLocalParentSpanId(null);
            }
            
            if (StringUtils.isNotEmpty(traceCtx.getCacheSpanName()))
            {
                traceCtx.setSpanName(traceCtx.getCacheSpanName());
                traceCtx.setCacheSpanName("");
            }
        }
    }
    
    @Override
    public void afterExcepHandler(Tracer tracer, Throwable e)
    {
        TraceContext traceCtx = tracer.getCurrentTraceContext().get();
        if (null != traceCtx)
        {
            String traceId = traceCtx.getTraceId();
            if (StringUtils.isEmpty(traceId))
            {
                throw new NullPointerException("Trace Id Is Null");
            }
            traceCtx.setType(ReportRequestType.CR);
            
            //module name + top method
            traceCtx.setAnnotation(ExceptionUtils.getStackTrace(e));
            Span span = tracer.newSpan(false, "0", true);
            //System.out.println(span);
            SenderTool.sendSpan(tracer.getReporter(), span);
            traceCtx.setAnnotation("");
            if (StringUtils.isNotEmpty(traceCtx.getLocalParentSpanId()))
            {
                traceCtx.setSpanId(traceCtx.getLocalParentSpanId());
                traceCtx.setLocalParentSpanId(null);
            }
            
            if (StringUtils.isNotEmpty(traceCtx.getCacheSpanName()))
            {
                traceCtx.setSpanName(traceCtx.getCacheSpanName());
                traceCtx.setCacheSpanName("");
            }
        }
    }
    
}