package cn.migu.trace.httpclient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import com.google.common.base.Joiner;

import cn.migu.trace.context.Tracer;
import cn.migu.trace.instrument.IClientInteceptor;

public final class HttpClient
{
    
    private IClientInteceptor inteceptor;
    
    private Tracer tracer;
    
    public HttpClient(Tracer tracer, IClientInteceptor inteceptor)
    {
        this.tracer = tracer;
        this.inteceptor = inteceptor;
    }
    
    /**
     * 请求配置
     */
    private static RequestConfig requestConfig =
        RequestConfig.custom().setSocketTimeout(10800000).setConnectTimeout(10800000).build();
    
    /**
     * http post请求
     * @param url
     * @param params
     * @return
     * @throws Exception
     * @see [类、类#方法、类#成员]
     */
    public String post(String url, Map<String, String> params)
        throws Exception
    {
        return post(url, params, null);
    }
    
    /**
     * http post请求
     * @param url
     * @param params
     * @param traceName
     * @return
     * @throws Exception
     * @see [类、类#方法、类#成员]
     */
    public String post(String url, Map<String, String> params, String traceName)
        throws Exception
    {
        List<NameValuePair> nvps = new ArrayList<NameValuePair>();
        
        Set<String> keySet = params.keySet();
        for (String key : keySet)
        {
            nvps.add(new BasicNameValuePair(key, params.get(key)));
        }
        
        return post(url, new UrlEncodedFormEntity(nvps, StandardCharsets.UTF_8), traceName);
    }
    
    /**
     * http post请求
     * @param url
     * @param entity
     * @return
     * @throws Exception
     * @see [类、类#方法、类#成员]
     */
    public String post(String url, UrlEncodedFormEntity entity)
        throws Exception
    {
        return post(url, entity, requestConfig, null);
    }
    
    public String post(String url, UrlEncodedFormEntity entity, String traceName)
        throws Exception
    {
        return post(url, entity, requestConfig, traceName);
    }
    
    public String post(String url, UrlEncodedFormEntity entity, RequestConfig cusConfig, String traceName)
        throws Exception
    {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpPost httpPost = null;
        
        try
        {
            List<Header> headerList = getHeadersList();
            
            if (null != inteceptor)
            {
                String anno = StringUtils.join("request url:", url, " post parameters:", parseParameters(entity));
                inteceptor.preHandler(tracer, headerList, traceName, "HttpClient", anno);
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
            
            httpPost.setHeaders(headerList.toArray(new Header[0]));
            
            httpPost.setEntity(entity);
            
            String result = httpClient.execute(httpPost,
                new ResponseHandlerImpl(StandardCharsets.UTF_8.toString(), inteceptor, tracer));
            
            return result;
        }
        catch (Exception e)
        {
            if (!(e instanceof HttpResponseException))
            {
                if (null != this.inteceptor)
                {
                    inteceptor.afterExcepHandler(tracer, e);
                }
            }
            
            throw e;
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
    
    private List<Header> getHeadersList()
    {
        ArrayList<Header> headers = new ArrayList<Header>();
        headers.add(new BasicHeader("Accept", "text/html, text/json, text/xml, html/text, */*"));
        headers.add(new BasicHeader("Accept-Language", "zh-cn,en-us,zh-tw,en-gb,en;"));
        headers.add(new BasicHeader("Accept-Charset", "gbk,gb2312,utf-8,BIG5,ISO-8859-1;"));
        headers.add(new BasicHeader("Connection", "keep-alive"));
        headers.add(new BasicHeader("Cache-Control", "no-cache"));
        headers.add(new BasicHeader("Accept-Encoding", "gzip"));
        headers.add(new BasicHeader("Content-Type", "application/x-www-form-urlencoded; charset=utf-8"));
        headers.add(new BasicHeader("User-Agent",
            "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1; .NET CLR 2.0.50727; CIBA)"));
        return headers;
    }
    
    /**
     * 请求head
     * @return
     * @throws Exception
     * @see [类、类#方法、类#成员]
     */
    public Header[] getHeaders(Map<String, String> extra)
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
    
    private String parseParameters(UrlEncodedFormEntity entity)
    {
        Map<String, String> postForm = new HashMap<String, String>();
        
        try
        {
            List<NameValuePair> nvps = URLEncodedUtils.parse(entity);
            for (NameValuePair nvp : nvps)
            {
                postForm.put(nvp.getName(), nvp.getValue());
            }
        }
        catch (IOException e)
        {
            
            e.printStackTrace();
            return null;
        }
        
        if (postForm.size() > 0)
        {
            Map<String, String> filteMap =
                postForm.entrySet().stream().filter(e -> StringUtils.isNotEmpty(e.getValue())).collect(
                    Collectors.toMap(p -> p.getKey(), p -> p.getValue()));
            return Joiner.on("|").withKeyValueSeparator(":").join(filteMap);
        }
        
        return "";
    }
}

/*
 * 响应处理类
 */
class ResponseHandlerImpl implements ResponseHandler<String>
{
    
    private String encode;
    
    private IClientInteceptor inteceptor;
    
    private Tracer tracer;
    
    public ResponseHandlerImpl(String encode, IClientInteceptor inteceptor, Tracer tracer)
    {
        this.tracer = tracer;
        this.inteceptor = inteceptor;
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
            
            if (null != this.inteceptor)
            {
                if (StringUtils.contains(responseBody, "失败") || StringUtils.contains(responseBody, "异常"))
                {
                    inteceptor.afterExcepHandler(tracer, new IllegalStateException(responseBody));
                }
                else
                {
                    inteceptor.afterHandler(tracer, responseBody);
                }
                
            }
        }
        else
        {
            HttpResponseException excep = new HttpResponseException(statusLine.getStatusCode(),
                StringUtils.join(String.valueOf(statusLine.getStatusCode()), ":", statusLine.getReasonPhrase()));
            if (null != this.inteceptor)
            {
                inteceptor.afterExcepHandler(tracer, excep);
            }
            
            throw excep;
        }
        
        return responseBody;
    }
    
}
