package cn.migu;

import cn.migu.trace.context.TraceContext;
import cn.migu.trace.context.Tracer;
import cn.migu.trace.context.Tracing;

public class MainTest
{
    public static void main(String[] args)
        throws InterruptedException
    {
        Tracer tracer1 = Tracing.newBuilder().build().tracer();
        Tracer tracer2 = Tracing.newBuilder().build().tracer();
        System.out.println(tracer1);
        System.out.println(tracer2);
        new Thread(new Runnable()
        {
            
            @Override
            public void run()
            {
                TraceContext tc = new TraceContext();
                tc.setTraceId("1232232323");
                long t1 = System.currentTimeMillis();
                //tracer1.currentTraceContext.newScope(tc);
                
                //System.out.println(tracer1.currentTraceContext.get().getTraceId());
                //System.out.println(tracer1.newSpan(""));
                long t2 = System.currentTimeMillis();
                System.out.println("1=>" + (t2 - t1));
                
            }
        }).start();
        new Thread(new Runnable()
        {
            
            @Override
            public void run()
            {
                TraceContext tc = new TraceContext();
                tc.setTraceId("2232232323a");
                long t1 = System.currentTimeMillis();
                tracer2.addTraceContext(tc);
                //System.out.println(tracer2.currentTraceContext.get().getTraceId());
                System.out.println(tracer2.newSpan(true));
                long t2 = System.currentTimeMillis();
                System.out.println("2=>" + (t2 - t1));
            }
        }).start();
        Thread.sleep(5000);
        
    }
}
