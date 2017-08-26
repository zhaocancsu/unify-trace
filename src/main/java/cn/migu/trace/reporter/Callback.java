package cn.migu.trace.reporter;

public interface Callback
{
    void onComplete();
    
    void onError(Throwable t);
}
