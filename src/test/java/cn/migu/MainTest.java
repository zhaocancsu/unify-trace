package cn.migu;

import cn.migu.trace.context.TraceContext;
import cn.migu.trace.context.Tracer;
import cn.migu.trace.context.Tracing;

public class MainTest
{
    public static void main(String[] args)
        throws InterruptedException
    {
        Tracing traing = Tracing.newBuilder().server("http://traceserver:8080/tracing/span").build();
        Tracer tracer1 = traing.tracer();
        Tracer tracer2 = traing.tracer();
        System.out.println(tracer1);
        System.out.println(tracer2);
        new Thread(new Runnable()
        {
            
            @Override
            public void run()
            {
                TraceContext tc = new TraceContext();
                tc.setTraceId("1232232323");
                tc.setSpanName("1121");
                tc.setType("cs");
                long t1 = System.currentTimeMillis();
                tracer1.addTraceContext(tc);
                
                System.out.println(tracer1.getCurrentTraceContext().get().getTraceId());
                System.out.println(tracer1.newSpan(true, "0", false));
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
                tc.setSpanName("ab");
                tc.setType("cr");
                long t1 = System.currentTimeMillis();
                tracer2.addTraceContext(tc);
                //System.out.println(tracer2.currentTraceContext.get().getTraceId());
                System.out.println(tracer2.newSpan(true, "0", false));
                long t2 = System.currentTimeMillis();
                System.out.println("2=>" + (t2 - t1));
            }
        }).start();
        new Thread(new Runnable()
        {
            
            @Override
            public void run()
            {
                TraceContext tc = new TraceContext();
                tc.setTraceId("ccvcvcvcvcv");
                tc.setSpanName("345f");
                tc.setType("ss");
                long t1 = System.currentTimeMillis();
                tracer2.addTraceContext(tc);
                //System.out.println(tracer2.currentTraceContext.get().getTraceId());
                System.out.println(tracer2.newSpan(true, "0", false));
                long t2 = System.currentTimeMillis();
                System.out.println("3=>" + (t2 - t1));
            }
        }).start();
        new Thread(new Runnable()
        {
            
            @Override
            public void run()
            {
                TraceContext tc = new TraceContext();
                tc.setTraceId("rtyuiop");
                tc.setSpanName("234r");
                tc.setType("sr");
                long t1 = System.currentTimeMillis();
                tracer2.addTraceContext(tc);
                //System.out.println(tracer2.currentTraceContext.get().getTraceId());
                try
                {
                    Thread.sleep(1000);
                }
                catch (InterruptedException e)
                {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                System.out.println(tracer2.newSpan(true, "0", false));
                long t2 = System.currentTimeMillis();
                System.out.println("4=>" + (t2 - t1));
            }
        }).start();
        Thread.sleep(5000);
        
    }
}
