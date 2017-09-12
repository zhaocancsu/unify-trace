package cn.migu.trace.instrument;

import java.io.UnsupportedEncodingException;
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

public class ClientInteceptor implements IClientInteceptor
{
    private String serviceName;
    
    public ClientInteceptor(String serviceName)
    {
        this.serviceName = serviceName;
    }
    
    @Override
    public void preHandler(Tracer tracer, List<Header> headers, String traceName, String funcInfixSpanName, String anno)
    {
        TraceContext traceCtx = tracer.getCurrentTraceContext().get();
        if (null != traceCtx)
        {
            
            long currentThreadId = Thread.currentThread().getId();
            if (currentThreadId != traceCtx.getThreadId())
            {
                try
                {
                    TraceContext childCtx = (TraceContext)traceCtx.clone();
                    
                    tracer.getCurrentTraceContext().newScope(childCtx);
                    
                    traceCtx = childCtx;
                }
                catch (CloneNotSupportedException e)
                {
                    e.printStackTrace();
                }
            }
            
            String traceId = traceCtx.getTraceId();
            if (StringUtils.isEmpty(traceId))
            {
                throw new NullPointerException("Trace Id Is Null");
            }
            traceCtx.setType(ReportRequestType.CS);
            
            //module name + top method
            String spanName = StringUtils.join(serviceName,
                "&",
                funcInfixSpanName,
                " ",
                Util.topMethodStack(Thread.currentThread().getStackTrace()));
            if (!StringUtils.isEmpty(traceCtx.getSpanName()))
            {
                traceCtx.setCacheSpanName(traceCtx.getSpanName());
            }
            traceCtx.setSpanName(spanName);
            
            if (StringUtils.isNotEmpty(traceCtx.getSpanId()))
            {
                traceCtx.setLocalParentSpanId(traceCtx.getSpanId());
            }
            
            if (StringUtils.isNotEmpty(anno))
            {
                traceCtx.setAnnotation(anno);
            }
            
            Span span = tracer.newSpan(false, "0", false);
            SenderTool.sendSpan(tracer.getReporter(), span);
            traceCtx.setSpanId(span.getSpanId());
            
            if (null != headers)
            {
                headers.add(new BasicHeader(PropagationKeys.TRACE_ID, traceId));
                headers.add(new BasicHeader(PropagationKeys.SPAN_ID, span.getSpanId()));
                addTraceNameToHeader(traceCtx, headers);
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
            ctx.setInstantaneous(true);
            String spanName = StringUtils.join(serviceName,
                "&",
                funcInfixSpanName,
                " ",
                Util.topMethodStack(Thread.currentThread().getStackTrace()));
            ctx.setSpanName(spanName);
            ctx.setType(ReportRequestType.CS);
            tracer.addTraceContext(ctx);
            if (StringUtils.isNotEmpty(anno))
            {
                ctx.setAnnotation(anno);
            }
            Span span = tracer.newSpan(true, "0", true);
            ctx.setSpanId(span.getSpanId());
            SenderTool.sendSpan(tracer.getReporter(), span);
            
            if (null != headers)
            {
                headers.add(new BasicHeader(PropagationKeys.TRACE_ID, span.getTraceId()));
                headers.add(new BasicHeader(PropagationKeys.SPAN_ID, span.getSpanId()));
                addTraceNameToHeader(ctx, headers);
            }
            
        }
        
    }
    
    @Override
    public void afterHandler(Tracer tracer, String anno)
    {
        TraceContext traceCtx = tracer.getCurrentTraceContext().get();
        if (null != traceCtx)
        {
            if (StringUtils.isNotEmpty(anno))
            {
                traceCtx.setAnnotation(anno);
            }
            String traceId = traceCtx.getTraceId();
            if (StringUtils.isEmpty(traceId))
            {
                throw new NullPointerException("Trace Id Is Null");
            }
            traceCtx.setType(ReportRequestType.CR);
            
            //module name + top method
            Span span = tracer.newSpan(false, "0", true);
            
            SenderTool.sendSpan(tracer.getReporter(), span);
            if (traceCtx.isInstantaneous())
            {
                tracer.getCurrentTraceContext().newScope(null);
            }
            else
            {
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
            Span span = tracer.newSpan(false, "1", true);
            SenderTool.sendSpan(tracer.getReporter(), span);
            
            if (traceCtx.isInstantaneous())
            {
                tracer.getCurrentTraceContext().newScope(null);
            }
            else
            {
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
    
    private void addTraceNameToHeader(TraceContext traceCtx, List<Header> headers)
    {
        if (StringUtils.isNotEmpty(traceCtx.getTraceName()))
        {
            try
            {
                headers.add(new BasicHeader(PropagationKeys.TRACE_NAME,
                    new String(traceCtx.getTraceName().getBytes(), "iso-8859-1")));
            }
            catch (UnsupportedEncodingException e)
            {
                e.printStackTrace();
            }
        }
    }
    
}
