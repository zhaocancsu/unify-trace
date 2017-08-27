package cn.migu.trace.reporter;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

public final class AwaitableCallback implements Callback
{
    final CountDownLatch countDown = new CountDownLatch(1);
    
    final AtomicReference<Throwable> throwable = new AtomicReference<>();
    
    public void await()
    {
        boolean interrupted = false;
        try
        {
            while (true)
            {
                try
                {
                    countDown.await();
                    Object result = throwable.get();
                    if (result == null)
                        return;
                    if (result instanceof Throwable)
                    {
                        if (result instanceof Error)
                            throw (Error)result;
                        if (result instanceof RuntimeException)
                            throw (RuntimeException)result;
                        // Don't set interrupted status when the callback received InterruptedException
                        throw new RuntimeException((Throwable)result);
                    }
                }
                catch (InterruptedException e)
                {
                    interrupted = true;
                }
            }
        }
        finally
        {
            if (interrupted)
            {
                Thread.currentThread().interrupt();
            }
        }
    }
    
    @Override
    public void onComplete()
    {
        countDown.countDown();
    }
    
    @Override
    public void onError(Throwable t)
    {
        throwable.set(t);
        countDown.countDown();
    }
}
