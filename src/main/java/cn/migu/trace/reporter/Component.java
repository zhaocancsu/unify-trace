package cn.migu.trace.reporter;

import java.io.Closeable;
import java.io.IOException;

import javax.annotation.Nullable;

import cn.migu.trace.internal.Util;

public interface Component extends Closeable
{
    Component.CheckResult check();
    
    void close()
        throws IOException;
    
    public static final class CheckResult
    {
        public static final Component.CheckResult OK = new Component.CheckResult(true, (Exception)null);
        
        public final boolean ok;
        
        @Nullable
        public final Exception exception;
        
        public static final Component.CheckResult failed(Exception exception)
        {
            return new Component.CheckResult(false, (Exception)Util.checkNotNull(exception, "exception"));
        }
        
        CheckResult(boolean ok, Exception exception)
        {
            this.ok = ok;
            this.exception = exception;
        }
    }
}