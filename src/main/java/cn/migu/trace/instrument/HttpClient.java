package cn.migu.trace.instrument;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import cn.migu.trace.context.ReportRequestType;
import cn.migu.trace.context.Span;
import cn.migu.trace.context.TraceContext;
import cn.migu.trace.context.Tracer;
import cn.migu.trace.context.Tracing;
import cn.migu.unify.comm.base.SpringContextUtil;

public final class HttpClient
{
    /**
     * 请求配置
     */
    private static RequestConfig requestConfig =
        RequestConfig.custom().setSocketTimeout(10800000).setConnectTimeout(10800000).build();
    
    private static Tracing tracing;
    
    /**
     * http post请求
     * @param url
     * @param params
     * @return
     * @throws Exception
     * @see [类、类#方法、类#成员]
     */
    public static String post(String url, Map<String, String> params, String serviceName)
        throws Exception
    {
        List<NameValuePair> nvps = new ArrayList<NameValuePair>();
        
        Set<String> keySet = params.keySet();
        for (String key : keySet)
        {
            nvps.add(new BasicNameValuePair(key, params.get(key)));
        }
        
        return post(url, new UrlEncodedFormEntity(nvps, StandardCharsets.UTF_8), serviceName);
    }
    
    /**
     * http post请求
     * @param url
     * @param entity
     * @return
     * @throws Exception
     * @see [类、类#方法、类#成员]
     */
    public static String post(String url, UrlEncodedFormEntity entity, String serviceName)
        throws Exception
    {
        
        return post(url, entity, requestConfig, getHeaders(), serviceName);
        
    }
    
    public static String post(String url, UrlEncodedFormEntity entity, RequestConfig cusConfig, Header[] header,
        String serviceName)
        throws Exception
    {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpPost httpPost = null;
        
        try
        {
            Object tracingObj = SpringContextUtil.getBean(Tracing.class);
            if (null != tracingObj)
            {
                if (null == tracing)
                {
                    tracing = (Tracing)tracingObj;
                }
                Tracer tracer = tracing.tracer();
                TraceContext traceCtx = tracer.getCurrentTraceContext().get();
                if (null != traceCtx)
                {
                    String traceId = traceCtx.getTraceId();
                    if (StringUtils.isEmpty(traceId))
                    {
                        throw new NullPointerException("Trace Id Is Null");
                    }
                    traceCtx.setType(ReportRequestType.CS);
                    
                    //module name + top method
                    
                    traceCtx.setSpanName(StringUtils.join(tracing.serviceName()));
                    Span span = tracer.newSpan(false);
                    System.out.println(span);
                }
                else
                {
                    TraceContext ctx = new TraceContext();
                    if (StringUtils.isNotEmpty(serviceName))
                    {
                        ctx.setSpanName(serviceName);
                    }
                    ctx.setSpanName(StringUtils.join(tracing.serviceName()));
                    ctx.setType(ReportRequestType.CS);
                    tracer.addTraceContext(ctx);
                    Span span = tracer.newSpan(true);
                    System.out.println(span);
                }
            }
            
            httpPost = new HttpPost(url);
            
            if (null != cusConfig)
            {
                httpPost.setConfig(cusConfig);
            }
            else
            {
                httpPost.setConfig(requestConfig);
            }
            
            if (null != header)
            {
                httpPost.setHeaders(header);
            }
            else
            {
                httpPost.setHeaders(getHeaders());
            }
            
            httpPost.setEntity(entity);
            
            String result = httpClient.execute(httpPost, new ResponseHandlerImpl(StandardCharsets.UTF_8.toString()));
            
            return result;
        }
        finally
        {
            if (null != httpPost)
            {
                httpPost.releaseConnection();
            }
            httpClient.close();
        }
    }
    
    /**
     * 请求head
     * @return
     * @throws Exception
     * @see [类、类#方法、类#成员]
     */
    private static Header[] getHeaders()
    {
        ArrayList<Header> headers = new ArrayList<Header>();
        headers.add(new BasicHeader("Accept", "text/html, text/json, text/xml, html/text, */*"));
        headers.add(new BasicHeader("Accept-Language", "zh-cn,en-us,zh-tw,en-gb,en;"));
        headers.add(new BasicHeader("Accept-Charset", "gbk,gb2312,utf-8,BIG5,ISO-8859-1;"));
        headers.add(new BasicHeader("Connection", "keep-alive"));
        headers.add(new BasicHeader("Cache-Control", "no-cache"));
        headers.add(new BasicHeader("Accept-Encoding", "gzip"));
        headers.add(new BasicHeader("User-Agent",
            "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1; .NET CLR 2.0.50727; CIBA)"));
        return headers.toArray(new Header[0]);
    }
    
    /**
     * 请求head
     * @return
     * @throws Exception
     * @see [类、类#方法、类#成员]
     */
    public static Header[] getHeaders(Map<String, String> extra)
    {
        ArrayList<Header> headers = new ArrayList<Header>();
        headers.add(new BasicHeader("Accept", "text/html, text/json, text/xml, html/text, */*"));
        headers.add(new BasicHeader("Accept-Language", "zh-cn,en-us,zh-tw,en-gb,en;"));
        headers.add(new BasicHeader("Accept-Charset", "gbk,gb2312,utf-8,BIG5,ISO-8859-1;"));
        headers.add(new BasicHeader("Connection", "keep-alive"));
        headers.add(new BasicHeader("Cache-Control", "no-cache"));
        headers.add(new BasicHeader("Accept-Encoding", "gzip"));
        headers.add(new BasicHeader("User-Agent",
            "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1; .NET CLR 2.0.50727; CIBA)"));
        if (null != extra && 0 != extra.size())
        {
            for (Map.Entry<String, String> entry : extra.entrySet())
            {
                headers.add(new BasicHeader(entry.getKey(), entry.getValue()));
            }
        }
        
        return headers.toArray(new Header[0]);
    }
}

/*
 * 响应处理类
 */
class ResponseHandlerImpl implements ResponseHandler<String>
{
    
    private String encode;
    
    public ResponseHandlerImpl(String encode)
    {
        this.encode = encode;
    }
    
    @Override
    public String handleResponse(HttpResponse response)
        throws ClientProtocolException, IOException
    {
        String responseBody = null;
        StatusLine statusLine = response.getStatusLine();
        int statusCode = statusLine.getStatusCode();
        
        if (statusCode >= HttpStatus.SC_OK && statusCode < HttpStatus.SC_MULTIPLE_CHOICES)
        {
            HttpEntity entity = response.getEntity();
            
            responseBody = (entity != null ? EntityUtils.toString(entity, this.encode) : null);
        }
        else
        {
            throw new HttpResponseException(statusLine.getStatusCode(),
                StringUtils.join(String.valueOf(statusLine.getStatusCode()), ":", statusLine.getReasonPhrase()));
        }
        
        return responseBody;
    }
    
}
