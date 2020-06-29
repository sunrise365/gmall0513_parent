package com.atguigu.gmall0513.realtime.util

object PropertiesUtil {

    import java.util.Properties

    def main(args: Array[String]): Unit = {
        import java.util.Properties
        val properties: Properties = PropertiesUtil.load("config.properties")

        // 通过 key 就能够得到对应的值，这个key在config.properties中定义了，配置的kafka服务器的地址
        println(properties.getProperty("kafka.broker.list"))
    }

    def load(propertieName:String): Properties ={
        import java.io.InputStreamReader
        val prop=new Properties();
        prop.load(new InputStreamReader(Thread.currentThread().getContextClassLoader.getResourceAsStream(propertieName) , "UTF-8"))
        prop
    }

}

