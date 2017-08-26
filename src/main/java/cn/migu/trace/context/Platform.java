package cn.migu.trace.context;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.logging.Logger;

public class Platform implements Clock
{
    static final Logger logger = Logger.getLogger(Tracer.class.getName());
    
    private static final Platform PLATFORM = new Platform();
    
    final long createTimestamp;
    
    final long createTick;
    
    Platform()
    {
        createTimestamp = System.currentTimeMillis() * 1000;
        createTick = System.nanoTime();
    }
    
    public static Platform get()
    {
        return PLATFORM;
    }
    
    public long randomLong()
    {
        return Math.abs(java.util.concurrent.ThreadLocalRandom.current().nextLong());
    }
    
    @Override
    public long currentTimeMicroseconds()
    {
        //return ((System.nanoTime() - createTick) / 1000)/* + createTimestamp*/;
        return System.currentTimeMillis();
    }
    
    public String hostAddr()
    {
        try
        {
            return InetAddress.getLocalHost().getHostAddress();
        }
        catch (UnknownHostException e)
        {
            return "unknow";
        }
    }
    
}
