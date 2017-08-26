package cn.migu.trace.context;

import java.io.Closeable;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

import javax.annotation.Nullable;

public abstract class CurrentTraceContext
{
    public abstract @Nullable TraceContext get();
    
    public abstract Scope newScope(@Nullable TraceContext currentSpan);
    
    public interface Scope extends Closeable
    {
        @Override
        void close();
    }
    
    public static final class Default extends CurrentTraceContext
    {
        static final InheritableThreadLocal<TraceContext> local = new InheritableThreadLocal<>();
        
        @Override
        public TraceContext get()
        {
            return local.get();
        }
        
        @Override
        public Scope newScope(TraceContext currentSpan)
        {
            final TraceContext previous = local.get();
            local.set(currentSpan);
            class DefaultCurrentTraceContextScope implements Scope
            {
                @Override
                public void close()
                {
                    local.set(previous);
                }
            }
            return new DefaultCurrentTraceContextScope();
        }
    }
    
    public <C> Callable<C> wrap(Callable<C> task)
    {
        final TraceContext invocationContext = get();
        class CurrentTraceContextCallable implements Callable<C>
        {
            @Override
            public C call()
                throws Exception
            {
                try (Scope scope = newScope(invocationContext))
                {
                    return task.call();
                }
            }
        }
        return new CurrentTraceContextCallable();
    }
    
    public Runnable wrap(Runnable task)
    {
        final TraceContext invocationContext = get();
        class CurrentTraceContextRunnable implements Runnable
        {
            @Override
            public void run()
            {
                try (Scope scope = newScope(invocationContext))
                {
                    task.run();
                }
            }
        }
        return new CurrentTraceContextRunnable();
    }
    
    public Executor executor(Executor delegate)
    {
        class CurrentTraceContextExecutor implements Executor
        {
            @Override
            public void execute(Runnable task)
            {
                delegate.execute(CurrentTraceContext.this.wrap(task));
            }
        }
        return new CurrentTraceContextExecutor();
    }
    
    public ExecutorService executorService(ExecutorService delegate)
    {
        class CurrentTraceContextExecutorService extends WrappingExecutorService
        {
            
            @Override
            protected ExecutorService delegate()
            {
                return delegate;
            }
            
            @Override
            protected <C> Callable<C> wrap(Callable<C> task)
            {
                return CurrentTraceContext.this.wrap(task);
            }
            
            @Override
            protected Runnable wrap(Runnable task)
            {
                return CurrentTraceContext.this.wrap(task);
            }
        }
        return new CurrentTraceContextExecutorService();
    }
}
