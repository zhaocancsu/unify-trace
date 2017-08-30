package cn.migu.trace.instrument;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import cn.migu.trace.context.PropagationKeys;
import cn.migu.trace.context.ReportRequestType;
import cn.migu.trace.context.Span;
import cn.migu.trace.context.TraceContext;
import cn.migu.trace.context.Tracer;
import cn.migu.trace.context.Tracing;

public class ControllerInterceptor extends HandlerInterceptorAdapter
{
    final Tracer tracer;
    
    public ControllerInterceptor(Tracing tracing)
    {
        this.tracer = tracing.tracer();
    }
    
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
        throws Exception
    {
        String reqURI = request.getRequestURI();
        Boolean isErrorPage = StringUtils.endsWith(request.getRequestURI(), "/error");
        if (isErrorPage)
        {
            return true;
        }
        
        String traceId = request.getHeader(PropagationKeys.TRACE_ID);
        String traceName = request.getHeader(PropagationKeys.TRACE_NAME);
        boolean isRootSpan = true;
        TraceContext traceCtx = new TraceContext();
        if (StringUtils.isNotEmpty(traceName))
        {
            traceCtx.setTraceName(traceName);
        }
        traceCtx.setSpanName(StringUtils.join("Controller:", reqURI));
        traceCtx.setType(ReportRequestType.SR);
        tracer.addTraceContext(traceCtx);
        if (!StringUtils.isEmpty(traceId))
        {
            isRootSpan = false;
            
            TraceContext eCtx = tracer.getCurrentTraceContext().get();
            eCtx.setTraceId(traceId);
            //eCtx.setSpanId(request.getHeader(PropagationKeys.SPAN_ID));
            eCtx.setInheritedSpanId(request.getHeader(PropagationKeys.SPAN_ID));
        }
        try
        {
            Span span = tracer.newSpan(isRootSpan, "0", false);
            //System.out.println(span);
            SenderTool.sendSpan(tracer.getReporter(), span);
            traceCtx.setSpanId(span.getSpanId());
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        
        System.out.println("Controller Thread id:" + Thread.currentThread().getId() + ",TraceContext="
            + tracer.getCurrentTraceContext().get().hashCode() + ",ctx=" + tracer.getCurrentTraceContext().get());
        
        request.setAttribute(PropagationKeys.TRACER_KEY, tracer);
        return true;
    }
    
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
        throws Exception
    {
        if (StringUtils.endsWith(request.getRequestURI(), "/error"))
        {
            return;
        }
        
        TraceContext traceCtx = tracer.getCurrentTraceContext().get();
        
        String annoStr = traceCtx.getAnnotation();
        
        String status = "0";
        
        if (null != ex)
        {
            annoStr = ExceptionUtils.getStackTrace(ex);
            status = "1";
        }
        
        traceCtx.setAnnotation(annoStr);
        traceCtx.setType(ReportRequestType.SS);
        
        try
        {
            
            if (null != traceCtx)
            {
                Span span = tracer.newSpan(false, status, true);
                SenderTool.sendSpan(tracer.getReporter(), span);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        
        request.removeAttribute(PropagationKeys.TRACER_KEY);
        traceCtx.setAnnotation("");
        traceCtx.setLocalParentSpanId(null);
        
        System.out.println("Controller Thread id:" + Thread.currentThread().getId() + ",TraceContext="
            + tracer.getCurrentTraceContext().get().hashCode() + ",ctx=" + tracer.getCurrentTraceContext().get());
    }
    
}