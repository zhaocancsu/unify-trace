package cn.migu.trace.instrument;

import cn.migu.trace.context.Span;
import cn.migu.trace.reporter.AwaitableCallback;
import cn.migu.trace.reporter.Sender;

public class SenderTool
{
    public static void sendSpan(Sender sender, Span span)
    {
        AwaitableCallback callback = new AwaitableCallback();
        sender.sendSpans(span, callback);
        callback.await();
    }
}
