package cn.migu.trace.reporter;

import static cn.migu.trace.internal.Util.checkArgument;
import static cn.migu.trace.internal.Util.checkNotNull;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.beanutils.BeanUtils;

import com.google.auto.value.AutoValue;

import cn.migu.trace.context.Span;
import okhttp3.Call;
import okhttp3.Dispatcher;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.internal.Util;

@AutoValue
public abstract class OkHttpSender implements Sender
{
    
    public static OkHttpSender create(String endpoint)
    {
        return builder().endpoint(endpoint).build();
    }
    
    public static Builder builder()
    {
        return new AutoValue_OkHttpSender.Builder().maxRequests(64).messageMaxBytes(5 * 1024 * 1024);
    }
    
    @AutoValue.Builder
    public static abstract class Builder
    {
        
        public final Builder endpoint(String endpoint)
        {
            checkNotNull(endpoint, "endpoint ex: http://traceserver:8080/tracing/span");
            HttpUrl parsed = HttpUrl.parse(endpoint);
            checkArgument(parsed != null, "invalid post url: " + endpoint);
            return endpoint(parsed);
        }
        
        public abstract Builder endpoint(HttpUrl endpoint);
        
        /** Maximum size of a message. Default 5MiB */
        public abstract Builder messageMaxBytes(int messageMaxBytes);
        
        /** Maximum in-flight requests. Default 64 */
        public abstract Builder maxRequests(int maxRequests);
        
        public abstract OkHttpClient.Builder clientBuilder();
        
        abstract int maxRequests();
        
        public final OkHttpSender build()
        {
            ThreadPoolExecutor dispatchExecutor = new ThreadPoolExecutor(0, maxRequests(), 60, TimeUnit.SECONDS,
                new SynchronousQueue<>(), Util.threadFactory("OkHttpSender Dispatcher", false));
            Dispatcher dispatcher = new Dispatcher(dispatchExecutor);
            dispatcher.setMaxRequests(maxRequests());
            dispatcher.setMaxRequestsPerHost(maxRequests());
            clientBuilder().dispatcher(dispatcher).build();
            
            return this.autoBuild();
            
        }
        
        abstract OkHttpSender autoBuild();
        
        Builder()
        {
        }
    }
    
    public final Builder toBuilder()
    {
        return new AutoValue_OkHttpSender.Builder().endpoint(endpoint())
            .maxRequests(client().dispatcher().getMaxRequests())
            .messageMaxBytes(messageMaxBytes());
    }
    
    abstract HttpUrl endpoint();
    
    abstract OkHttpClient client();
    
    abstract int maxRequests();
    
    volatile boolean closeCalled;
    
    @Override
    public void sendSpans(Span span, Callback callback)
    {
        if (closeCalled)
            throw new IllegalStateException("closed");
        try
        {
            FormBody.Builder formBuilder = new FormBody.Builder();
            
            Map<String, String> map = BeanUtils.describe(span);
            map.remove("class");
            for (Map.Entry<String, String> entry : map.entrySet())
            {
                String k = entry.getKey();
                String v = (null == entry.getValue()) ? "" : entry.getValue();
                formBuilder.add(k, v);
            }
            
            RequestBody requestBody = formBuilder.build();
            
            Request request = newRequest(requestBody);
            client().newCall(request).enqueue(new CallbackAdapter(callback));
        }
        catch (Throwable e)
        {
            callback.onError(e);
            if (e instanceof Error)
                throw (Error)e;
        }
    }
    
    @Override
    public CheckResult check()
    {
        try
        {
            Request request = new Request.Builder().url(endpoint())
                .post(RequestBody.create(MediaType.parse("application/json"), "[]"))
                .build();
            try (Response response = client().newCall(request).execute())
            {
                if (!response.isSuccessful())
                {
                    throw new IllegalStateException("check response failed: " + response);
                }
            }
            return CheckResult.OK;
        }
        catch (Exception e)
        {
            return CheckResult.failed(e);
        }
    }
    
    @Override
    public void close()
    {
        if (closeCalled)
            return;
        closeCalled = true;
        
        Dispatcher dispatcher = client().dispatcher();
        dispatcher.executorService().shutdown();
        try
        {
            if (!dispatcher.executorService().awaitTermination(1, TimeUnit.SECONDS))
            {
                dispatcher.cancelAll();
            }
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
        }
    }
    
    Request newRequest(RequestBody body)
        throws IOException
    {
        Request.Builder request = new Request.Builder().url(endpoint());
        
        request.post(body);
        return request.build();
    }
    
    @Override
    public final String toString()
    {
        return "OkHttpSender(" + endpoint() + ")";
    }
    
    OkHttpSender()
    {
    }
    
    static final class CallbackAdapter implements okhttp3.Callback
    {
        final Callback delegate;
        
        CallbackAdapter(Callback delegate)
        {
            this.delegate = delegate;
        }
        
        @Override
        public void onFailure(Call call, IOException e)
        {
            delegate.onError(e);
        }
        
        @Override
        public void onResponse(Call call, Response response)
            throws IOException
        {
            try (ResponseBody responseBody = response.body())
            {
                if (response.isSuccessful())
                {
                    delegate.onComplete();
                }
                else
                {
                    delegate.onError(new IllegalStateException("response failed: " + response));
                }
            }
        }
        
        @Override
        public String toString()
        {
            return "CallbackAdapter(" + delegate + ")";
        }
    }
}
