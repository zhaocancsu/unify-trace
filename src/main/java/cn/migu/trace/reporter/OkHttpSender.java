package cn.migu.trace.reporter;

import static cn.migu.trace.internal.Util.checkArgument;
import static cn.migu.trace.internal.Util.checkNotNull;

import java.io.IOException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Dispatcher;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.internal.Util;
import okio.Buffer;
import okio.BufferedSink;

import org.omg.IOP.Encoding;

import cn.migu.trace.context.Span;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class OkHttpSender implements Sender
{
    
    public static OkHttpSender create(String endpoint)
    {
        return builder().endpoint(endpoint).build();
    }
    
    public static Builder builder()
    {
        return new AutoValue_OkHttpSender.Builder().encoding(Encoding.THRIFT)
            .compressionEnabled(true)
            .maxRequests(64)
            .messageMaxBytes(5 * 1024 * 1024);
    }
    
    @AutoValue.Builder
    public static abstract class Builder
    {
        
        /**
         * No default. The POST URL for zipkin's <a href="http://zipkin.io/zipkin-api/#/">v1 api</a>,
         * usually "http://zipkinhost:9411/api/v1/spans"
         */
        // customizable so that users can re-map /api/v1/spans ex for browser-originated traces
        public final Builder endpoint(String endpoint)
        {
            checkNotNull(endpoint, "endpoint ex: http://zipkinhost:9411/api/v1/spans");
            HttpUrl parsed = HttpUrl.parse(endpoint);
            checkArgument(parsed != null, "invalid post url: " + endpoint);
            return endpoint(parsed);
        }
        
        public abstract Builder endpoint(HttpUrl endpoint);
        
        /** Default true. true implies that spans will be gzipped before transport. */
        public abstract Builder compressionEnabled(boolean compressSpans);
        
        /** Maximum size of a message. Default 5MiB */
        public abstract Builder messageMaxBytes(int messageMaxBytes);
        
        /** Maximum in-flight requests. Default 64 */
        public abstract Builder maxRequests(int maxRequests);
        
        /** Controls the "Content-Type" header when sending spans. */
        //public abstract Builder encoding(Encoding encoding);
        
        public abstract OkHttpClient.Builder clientBuilder();
        
        abstract int maxRequests();
        
        //abstract Encoding encoding();
        
        public final OkHttpSender build()
        {
            // bound the executor so that we get consistent performance
            ThreadPoolExecutor dispatchExecutor = new ThreadPoolExecutor(0, maxRequests(), 60, TimeUnit.SECONDS,
            // Using a synchronous queue means messages will send immediately until we hit max
            // in-flight requests. Once max requests are hit, send will block the caller, which is
            // the AsyncReporter flush thread. This is ok, as the AsyncReporter has a buffer of
            // unsent spans for this purpose.
                new SynchronousQueue<>(), Util.threadFactory("OkHttpSender Dispatcher", false));
            Dispatcher dispatcher = new Dispatcher(dispatchExecutor);
            dispatcher.setMaxRequests(maxRequests());
            dispatcher.setMaxRequestsPerHost(maxRequests());
            clientBuilder().dispatcher(dispatcher).build();
            
            return this.autoBuild();
            /*if (encoding() == Encoding.JSON)
            {
                return encoder(RequestBodyMessageEncoder.JSON).autoBuild();
            }
            else if (encoding() == Encoding.THRIFT)
            {
                return encoder(RequestBodyMessageEncoder.THRIFT).autoBuild();
            }
            throw new UnsupportedOperationException("Unsupported encoding: " + encoding().name());*/
        }
        
        //abstract Builder encoder(RequestBodyMessageEncoder encoder);
        
        abstract OkHttpSender autoBuild();
        
        Builder()
        {
        }
    }
    
    /**
     * Creates a builder out of this object. Note: if the {@link Builder#clientBuilder()} was
     * customized, you'll need to re-apply those customizations.
     */
    public final Builder toBuilder()
    {
        return new AutoValue_OkHttpSender.Builder().endpoint(endpoint())
            .maxRequests(client().dispatcher().getMaxRequests())
            //.compressionEnabled(compressionEnabled())
            //.encoding(encoding())
            .messageMaxBytes(messageMaxBytes());
    }
    
    abstract HttpUrl endpoint();
    
    abstract OkHttpClient client();
    
    abstract int maxRequests();
    
    //abstract boolean compressionEnabled();
    
    //abstract RequestBodyMessageEncoder encoder();
    
    /*@Override
    public int messageSizeInBytes(List<byte[]> encodedSpans)
    {
        return encoding().listSizeInBytes(encodedSpans);
    }*/
    
    /** close is typically called from a different thread */
    volatile boolean closeCalled;
    
    /** Asynchronously sends the spans as a POST to {@link #endpoint()}. */
    @Override
    public void sendSpans(Span span, Callback callback)
    {
        if (closeCalled)
            throw new IllegalStateException("closed");
        try
        {
            RequestBody requestBody =
                new MultipartBody.Builder().setType(MultipartBody.FORM)
                    .addFormDataPart("somParam", "someValue")
                    .build();
            
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
    
    /** Sends an empty json message to the configured endpoint. */
    @Override
    public CheckResult check()
    {
        try
        {
            Request request =
                new Request.Builder().url(endpoint())
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
    
    /** Waits up to a second for in-flight requests to finish before cancelling them */
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
        /*if (compressionEnabled())
        {
            request.addHeader("Content-Encoding", "gzip");
            Buffer gzipped = new Buffer();
            BufferedSink gzipSink = Okio.buffer(new GzipSink(gzipped));
            body.writeTo(gzipSink);
            gzipSink.close();
            body = new BufferRequestBody(body.contentType(), gzipped);
        }*/
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
    
    static final class BufferRequestBody extends RequestBody
    {
        final MediaType contentType;
        
        final Buffer body;
        
        BufferRequestBody(MediaType contentType, Buffer body)
        {
            this.contentType = contentType;
            this.body = body;
        }
        
        @Override
        public long contentLength()
            throws IOException
        {
            return body.size();
        }
        
        @Override
        public MediaType contentType()
        {
            return contentType;
        }
        
        @Override
        public void writeTo(BufferedSink sink)
            throws IOException
        {
            sink.write(body, body.size());
        }
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
