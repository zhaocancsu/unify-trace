package cn.migu.trace.context;

import java.io.Closeable;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Nullable;

public abstract class Tracing implements Closeable
{
    
    public static Builder newBuilder()
    {
        return new Builder();
    }
    
    abstract public Tracer tracer();
    
    abstract public CurrentTraceContext currentTraceContext();
    
    static volatile Tracing current = null;
    
    @Nullable
    public static Tracer currentTracer()
    {
        Tracing tracing = current;
        return tracing != null ? tracing.tracer() : null;
    }
    
    final AtomicBoolean noop = new AtomicBoolean(false);
    
    @Nullable
    public static Tracing current()
    {
        return current;
    }
    
    @Override
    abstract public void close();
    
    public static final class Builder
    {
        
        CurrentTraceContext currentTraceContext = new CurrentTraceContext.Default();
        
        public Builder currentTraceContext(CurrentTraceContext currentTraceContext)
        {
            this.currentTraceContext = currentTraceContext;
            return this;
        }
        
        public Tracing build()
        {
            
            return new Default(this);
        }
        
        Builder()
        {
        }
    }
    
    static final class Default extends Tracing
    {
        final Tracer tracer;
        
        final CurrentTraceContext currentTraceContext;
        
        Default(Builder builder)
        {
            this.tracer = new Tracer(builder);
            this.currentTraceContext = builder.currentTraceContext;
            maybeSetCurrent();
        }
        
        @Override
        public Tracer tracer()
        {
            return tracer;
        }
        
        @Override
        public CurrentTraceContext currentTraceContext()
        {
            return currentTraceContext;
        }
        
        private void maybeSetCurrent()
        {
            if (current != null)
                return;
            synchronized (Tracing.class)
            {
                if (current == null)
                    current = this;
            }
        }
        
        @Override
        public void close()
        {
            if (current != this)
                return;
            
            synchronized (Tracing.class)
            {
                if (current == this)
                    current = null;
            }
        }
    }
    
}
