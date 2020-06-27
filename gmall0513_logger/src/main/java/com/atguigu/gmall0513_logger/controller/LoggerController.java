package com.atguigu.gmall0513_logger.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall0513.common.constants.GmallConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


/**
 *  需要处理的数据格式为：   http://logserver/log?logString={"area":"hebei","uid":"17","os":“andriod"………………
 *      前面的logserver是如何找到服务器，是服务器的名字， 后面的log才是服务器接收到请求的请求路径，
 */
@RestController
@Slf4j  // 在编译的时候就会帮你自动生成log的对象，通过log可以输出各种各样的日志，都不需要声明的，这个注解就直接帮你添加了一行声明和实例化的对象的语句，这样你就能直接写语句了
public class LoggerController {

    //第三个问题，就是如何将日志文件发送到kafka中（现在已经可以在控制台，包括已经落盘了）
    // 因为一共有两种日志类型（启动日志和时间日志），那么就可以根据不同的日志类型发送不同的kafka主题中.区分的最主要的问题是 type： 一个是event，一个是startup
    //因为在SPRINGBOOT创建的时候已经导入了KAFKA的POM坐标，这是一个SPRING-KAFKA的集成JAR包，不需要写KAFKA原生的PRODUCER之类的东西了
    @Autowired  // 这是一个接口，会帮你自动实例化这个对象
    KafkaTemplate<String,String> kafkaTemplate;


    // 如果你写了 return “success”，但是没有写 @ResponseBody的注解，springboot回去找一个叫做success的页面，而不是给浏览器返回一个success的字符串
    // 如果不写@ResponseBody， 可以直接在类上写，@RestController， 这个注解就相当于 @ResponseBody + @Controller

    // 这里一般写的是 @RequestMapping，但是现在是post请求，可以直接用@PostMapping 来代替,
    // 但是有一点需要注意，如果是@RequestMapping，在测试的时候页面是返回 success的，但是如果使用的是@PostMapping，就会返回404，找不到页面，说明success没有直接返回到页面上
    // 但是经过测试，使用PostMapping这个注解也不会跳转到success页面，这个需要研究研究
    // @RequestMapping("/log")
    @PostMapping("/log")
    public String Log(@RequestParam("logString") String logString){

        //如果使用FastJson的话，这个logger的pom文件中是没有的，但是common模块中有啊，所以需要在logger模块中导入common模块
        JSONObject jsonObject = JSON.parseObject(logString);
        jsonObject.put("ts",System.currentTimeMillis());

        if ("startup".equals(jsonObject.get("type"))){
            // 第一个参数是主题，第二个参数是数据, 注意，这里的主题尽量不要写死了，可以在common中定义一个公共的常量类
            kafkaTemplate.send(GmallConstant.KAFKA_STARTUP,jsonObject.toJSONString());
        }else {
            kafkaTemplate.send(GmallConstant.KAFKA_EVENT,jsonObject.toJSONString());
        }


        // 使用@Slf4j，将日志落盘
        // 输出到什么位置上，取决于log是怎么配置的，（可以在resources中添加一个文件： log4j.properties---这个配置在下面的1.3.5）
        // 在配置完了以后还是不能运行的，因为springboot的默认使用的是logging中的lomback，所以需要把pom文件中的logging排除掉，使用log4j
        log.info(logString);

        //System.out.println(logString);
        return "success";
    }
}
