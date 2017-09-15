package cn.migu.trace.linuxcmd;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.Session;
import ch.ethz.ssh2.StreamGobbler;
import cn.migu.commons.OperateLog;
import cn.migu.commons.Bean.OperateLogBean;
import cn.migu.trace.context.Tracer;
import cn.migu.trace.instrument.IClientInteceptor;
import cn.migu.unify.comm.base.ExceptionUtil;

public class Shell
{
    private static final Log errorLog = LogFactory.getLog("error");
    
    private IClientInteceptor inteceptor;
    
    private Tracer tracer;
    
    public Shell(Tracer tracer, IClientInteceptor inteceptor)
    {
        this.tracer = tracer;
        this.inteceptor = inteceptor;
    }
    
    public String execCommandByJar(BigDecimal kind, String hostname, int port, String username, String password,
        String[] commands)
        throws Exception
    {
        if (null != this.inteceptor)
        {
            String cmd = "";
            if (null != commands && commands.length > 0)
            {
                cmd = StringUtils.join(commands, ",");
            }
            String anno = StringUtils.join(hostname, ":", String.valueOf(port), " ", username, "/", password, " ", cmd);
            inteceptor.preHandler(tracer, null, "", "RemoteLinuxShell", anno);
        }
        String resultValue = "";
        //指明连接主机的IP地址
        System.out.println(
            "hostName-----" + hostname + ",port---" + port + ",username---" + username + ",password---" + password);
        Connection conn = new Connection(hostname, port);
        Session ssh = null;
        try
        {
            //连接到主机
            conn.connect();
            //使用用户名和密码校验
            boolean isconn = conn.authenticateWithPassword(username, password);
            if (!isconn)
            {
                resultValue = "用户名或密码不正确";
                System.out.println("用户名或密码不正确");
            }
            else
            {
                if (commands != null && commands.length > 0)
                {
                    for (int i = 0; i < commands.length; i++)
                    {
                        ssh = conn.openSession();
                        if (ssh != null)
                        {
                            // 执行Linux命令
                            //                            System.out.println(commands[i]);
                            ssh.execCommand(commands[i]);
                            System.out.println("----" + commands[i]);
                            InputStream stdErr = new StreamGobbler(ssh.getStderr());
                            BufferedReader brs = null;
                            if (new BigDecimal(3).equals(kind) || new BigDecimal(2).equals(kind)
                                || new BigDecimal(6).equals(kind) || new BigDecimal(7).equals(kind))
                            {
                                InputStream stdout = new StreamGobbler(ssh.getStdout());
                                if (stdout != null)
                                {
                                    brs = new BufferedReader(new InputStreamReader(stdout));
                                    while (null != brs)
                                    {
                                        String line = brs.readLine();
                                        OperateLogBean operateLogBean = new OperateLogBean();
                                        operateLogBean.setExtraInfo("启动日志-正常---" + line);
                                        OperateLog.getInstance().error(errorLog, operateLogBean);
                                        System.out.println("正常----------" + line);
                                        if (line == null)
                                        {
                                            brs.close();
                                            break;
                                        }
                                        else if (line.indexOf("ZDY-Start-Successfully") > -1)
                                        {
                                            resultValue = "启动成功";
                                            brs.close();
                                            break;
                                        }
                                        else if (line.indexOf("Address already in use") > -1)
                                        {
                                            resultValue = "端口占用";
                                            brs.close();
                                            break;
                                        }
                                        else if (line.indexOf("SQJ-Source-Path-Not-Exists") > -1)
                                        {
                                            resultValue = "原始路径有误";
                                            brs.close();
                                            break;
                                        }
                                        else if (line.indexOf("SQJ-Collect-Path-Not-Exists") > -1)
                                        {
                                            resultValue = "采集路径有误";
                                            brs.close();
                                            break;
                                        }
                                        else if (line.indexOf("args length less than 8") > -1)
                                        {
                                            resultValue = "输入参数少于8个";
                                            brs.close();
                                            break;
                                        }
                                        else if (line.indexOf("SPRINGBOOT-SUCCESS:") > -1)
                                        {
                                            resultValue = line;
                                            brs.close();
                                            break;
                                        }
                                        else if (line.indexOf("cleanjar Start Successfully") > -1)
                                        {
                                            resultValue = line;
                                            brs.close();
                                            break;
                                        }
                                        else if (line.indexOf("cleanjar Start failed") > -1)
                                        {
                                            resultValue = "启动失败";
                                            brs.close();
                                            break;
                                        }
                                        else if (line.indexOf("cleanjar args length less than 4") > -1)
                                        {
                                            resultValue = "参数长度不能小于4个,顺序为appID,serviceID,port,jarID";
                                            brs.close();
                                            break;
                                        }
                                    }
                                }
                            }
                            if (null == resultValue || "".equals(resultValue) || new BigDecimal(1).equals(kind)
                                || new BigDecimal(4).equals(kind) || new BigDecimal(5).equals(kind))
                            {
                                if (stdErr != null)
                                {
                                    brs = new BufferedReader(new InputStreamReader(stdErr));
                                    while (brs != null && true)
                                    {
                                        String line = brs.readLine();
                                        OperateLogBean operateLogBean = new OperateLogBean();
                                        operateLogBean.setExtraInfo("启动日志-错误---" + line);
                                        OperateLog.getInstance().error(errorLog, operateLogBean);
                                        System.out.println("错误--------" + line);
                                        if (line == null)
                                        {
                                            brs.close();
                                            break;
                                        }
                                        else if (line.indexOf("No such file or directory") > -1)
                                        {
                                            resultValue = "找不到路径，请检查部署路径或采集路径是否正确";
                                            brs.close();
                                            break;
                                        }
                                        else if (line.indexOf("Directory does not exist") > -1)
                                        {
                                            resultValue = "采集目录不存在";
                                            brs.close();
                                            break;
                                        }
                                        else if (line.indexOf("Permission denied") > -1)
                                        {
                                            resultValue = "权限不足";
                                            brs.close();
                                            break;
                                        }
                                        else if (line.indexOf("Address already in use") > -1)
                                        {
                                            resultValue = "端口占用";
                                            brs.close();
                                            break;
                                        }
                                        else if (line.indexOf("Unable to access jarfile") > -1)
                                        {
                                            resultValue = "部署路径不存在";
                                            brs.close();
                                            break;
                                        }
                                        else if (line.indexOf("Initial job has not accepted any resources") > -1)
                                        {
                                            resultValue = "启动成功,但当前集群无可用资源";
                                            brs.close();
                                            break;
                                        }
                                        if (line.indexOf("SQJ-Monitored-Successfully") > -1)
                                        {
                                            resultValue = "启动成功";
                                            brs.close();
                                            break;
                                        }
                                        else if (line.indexOf("STREAING STARTED") > -1)
                                        {
                                            resultValue = "启动成功";
                                            brs.close();
                                            break;
                                        }
                                        else if (line.indexOf("SPRINGBOOT-ERROR:") > -1)
                                        {
                                            resultValue = "该JAR已启动或启动有误";
                                            brs.close();
                                            break;
                                        }
                                    }
                                }
                                ssh.close();
                            }
                        }
                    }
                }
            }
            //连接的Session和Connection对象都需要关闭
            conn.close();
            
            if (null != this.inteceptor)
            {
                if (StringUtils.isNotEmpty(resultValue) && !StringUtils.equals(resultValue, "启动成功")
                    && !StringUtils.contains(resultValue, "Start Successfully"))
                {
                    inteceptor.afterExcepHandler(tracer, new IllegalStateException(resultValue));
                }
                else
                {
                    inteceptor.afterHandler(tracer, "");
                }
                
            }
        }
        catch (Exception e)
        {
            if (conn != null)
            {
                conn.close();
                conn = null;
            }
            if (null != this.inteceptor)
            {
                inteceptor.afterExcepHandler(tracer, e);
            }
            
            throw e;
        }
        
        return resultValue;
    }
    
    public void execCommandByPort(String hostname, int port, String username, String password, String command)
    {
        //指明连接主机的IP地址
        Connection conn = new Connection(hostname, port);
        Session ssh = null;
        String retStr = "";
        if (null != this.inteceptor)
        {
            String anno =
                StringUtils.join(hostname, ":", String.valueOf(port), " ", username, "/", password, " ", command);
            inteceptor.preHandler(tracer, null, "", "RemoteLinuxShell", anno);
        }
        try
        {
            //连接到主机
            conn.connect();
            //使用用户名和密码校验
            boolean isconn = conn.authenticateWithPassword(username, password);
            if (!isconn)
            {
                retStr = "用户名或密码不正确";
            }
            else
            {
                ssh = conn.openSession();
                if (ssh != null)
                {
                    // 执行Linux命令
                    ssh.execCommand(command);
                    InputStream is = new StreamGobbler(ssh.getStdout());
                    if (is != null)
                    {
                        // 读取命令执行返回打印在屏幕上的文字
                        BufferedReader brs = new BufferedReader(new InputStreamReader(is));
                        while (brs != null && true)
                        {
                            String tmp = brs.readLine();
                            if (tmp == null)
                            {
                                brs.close();
                                break;
                            }
                        }
                    }
                    ssh.close();
                }
            }
            //连接的Session和Connection对象都需要关闭
            conn.close();
            
            if (null != this.inteceptor)
            {
                if (StringUtils.isNotEmpty(retStr))
                {
                    inteceptor.afterExcepHandler(tracer, new IllegalStateException(retStr));
                }
                else
                {
                    inteceptor.afterHandler(tracer, "");
                }
                
            }
        }
        catch (Exception e)
        {
            OperateLogBean operateLogBean = new OperateLogBean();
            String exceptionStr = ExceptionUtil.resolveException(e);
            operateLogBean.setOperateResult("fail");
            operateLogBean.setSataticInfo("kafka");
            operateLogBean.setSystemModuleName("LinuxUtil");
            operateLogBean.setExtraInfo(exceptionStr);
            OperateLog.getInstance().error(errorLog, operateLogBean);
            e.printStackTrace();
            if (conn != null)
            {
                conn.close();
                conn = null;
            }
            
            if (null != this.inteceptor)
            {
                inteceptor.afterExcepHandler(tracer, e);
            }
        }
    }
}
