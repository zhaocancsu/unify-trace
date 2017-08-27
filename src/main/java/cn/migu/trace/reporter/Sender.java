package cn.migu.trace.reporter;

import cn.migu.trace.context.Span;

public interface Sender extends Component
{
    int messageMaxBytes();
    
    void sendSpans(Span span, Callback callback);
}
