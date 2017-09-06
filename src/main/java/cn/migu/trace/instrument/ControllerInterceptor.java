package cn.migu.trace.instrument;

import java.io.UnsupportedEncodingException;

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
    
    final String serviceName;
    
    public ControllerInterceptor(Tracing tracing)
    {
        this.tracer = tracing.tracer();
        this.serviceName = tracing.serviceName();
    }
    
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
    {
        String reqURI = request.getRequestURI();
        Boolean isErrorPage = StringUtils.endsWith(request.getRequestURI(), "/error");
        if (isErrorPage)
        {
            return true;
        }
        
        String traceId = request.getHeader(PropagationKeys.TRACE_ID);
        String midTraceName = request.getHeader(PropagationKeys.TRACE_NAME);
        String traceName = "";
        try
        {
            traceName = new String(midTraceName.getBytes("iso-8859-1"), "UTF-8");
        }
        catch (UnsupportedEncodingException e1)
        {
            e1.printStackTrace();
        }
        boolean isRootSpan = true;
        TraceContext traceCtx = new TraceContext();
        if (StringUtils.isNotEmpty(traceName))
        {
            traceCtx.setTraceName(traceName);
        }
        traceCtx.setSpanName(StringUtils.join(serviceName, "&Controller:", reqURI));
        traceCtx.setType(ReportRequestType.SR);
        tracer.addTraceContext(traceCtx);
        if (!StringUtils.isEmpty(traceId))
        {
            isRootSpan = false;
            
            TraceContext eCtx = tracer.getCurrentTraceContext().get();
            eCtx.setTraceId(traceId);
            
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
        
        request.setAttribute(PropagationKeys.TRACER_KEY, tracer);
        return true;
    }
    
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
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
        //traceCtx.setAnnotation("");
        //traceCtx.setLocalParentSpanId(null);
        
        tracer.getCurrentTraceContext().newScope(null);
        
    }
    
}