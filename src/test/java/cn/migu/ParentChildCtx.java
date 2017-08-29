package cn.migu;

import cn.migu.trace.context.TraceContext;
import cn.migu.trace.context.Tracer;
import cn.migu.trace.context.Tracing;

public class ParentChildCtx
{
    
    public static void main(String[] args)
        throws InterruptedException
    {
        Tracing tracing =
            Tracing.newBuilder().server("http://traceserver:8080/tracing/span").serviceName("test").build();
        Tracer tracer = tracing.tracer();
        TraceContext pCtx = new TraceContext();
        pCtx.setTraceId("11");
        pCtx.setType("22");
        pCtx.setAnnotation("erer");
        
        tracer.addTraceContext(pCtx);
        
        System.out.println(pCtx.hashCode());
        
        new Thread(new Runnable()
        {
            
            @Override
            public void run()
            {
                TraceContext cCtx = tracer.getCurrentTraceContext().get();
                System.out.println(cCtx.hashCode());
                System.out.println(cCtx.getAnnotation());
                
            }
        }).start();
        Thread.sleep(1000);
    }
    
}
