package com.atguigu.gmall0513.mock;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 *  发送日志的工具类
 */
public class LogUploader {

    public static void sendLogStream(String log){
        try{
            //不同的日志类型对应不同的URL
            // 往一个指定的日志服务器发送log，这个url是一个映射，是ip地址的映射，可以写在host里面
            //      192.168.1.101 logserver 在switchHost里面配置
            URL url = new URL("http://logserver/log");

            // 这个是打完jar包以后，在Linux系统中使用命令行的方式启动springboot，因为Linux系统中非root账户端口号不能超过1024，
            // 所以使用8080端口号，这里在进行单电测试的时候，也需要把端口号修改为8080
            //URL url = new URL("http://logserver:8080/log");

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            //设置请求方式为post
            conn.setRequestMethod("POST");

            //时间头用来供server进行时钟校对的
            conn.setRequestProperty("clientTime",System.currentTimeMillis() + "");
            //允许上传数据
            conn.setDoOutput(true);
            //设置请求的头信息,设置内容类型为JSON， 第二个参数是格式： form表单的格式，就是key和value的格式
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            System.out.println("upload" + log);

            //输出流， logString=这个就是key， value就是这个静态方法参数中传递过来的log
            OutputStream out = conn.getOutputStream();
            out.write(("logString="+log).getBytes());
            out.flush();
            out.close();
            int code = conn.getResponseCode();
            System.out.println(code);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

}