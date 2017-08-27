package cn.migu;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;

import org.apache.commons.beanutils.BeanUtils;

import cn.migu.trace.context.Span;

public class BeanToMapTest
{
    
    public static void main(String[] args)
        throws IllegalAccessException, InvocationTargetException, NoSuchMethodException
    {
        Span span = Span.create("1", "b", "01", "xx/a", "", "sr", "127.0.0.1", "15012312312", "");
        
        Map<String, String> map = BeanUtils.describe(span);
        map.remove("class");
        System.out.println(map);
        for (Map.Entry<?, ?> entry : map.entrySet())
        {
            String k = (String)entry.getKey();
            String v = (String)entry.getValue();
            System.out.println(k + "=" + v);
        }
    }
    
}
