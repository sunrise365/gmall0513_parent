package com.atguigu.gmall0513.realtime.util

import org.apache.kafka.common.serialization.StringDeserializer
import java.util.Properties

import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.spark.streaming.StreamingContext
import org.apache.spark.streaming.dstream.InputDStream
import org.apache.spark.streaming.kafka010.{ConsumerStrategies, KafkaUtils, LocationStrategies}

object MyKafkaUtil {

    private val properties: Properties = PropertiesUtil.load("config.properties")
    val broker_list = properties.getProperty("kafka.broker.list")

    // kafka消费者配置
    val kafkaParam = Map(
        "bootstrap.servers" -> broker_list,//用于初始化链接到集群的地址
        "key.deserializer" -> classOf[StringDeserializer],
        "value.deserializer" -> classOf[StringDeserializer],
        //用于标识这个消费者属于哪个消费团体
        "group.id" -> "gmall_consumer_group",
        //如果没有初始化偏移量或者当前的偏移量不存在任何服务器上，可以使用这个配置属性
        //可以使用这个配置，latest自动重置偏移量为最新的偏移量
        "auto.offset.reset" -> "latest",
        //如果是true，则这个消费者的偏移量会在后台自动提交,但是kafka宕机容易丢失数据
        //如果是false，会需要手动维护kafka偏移量
        "enable.auto.commit" -> (true: java.lang.Boolean)
    )

    // 创建DStream，返回接收到的输入数据
    // LocationStrategies：根据给定的主题和集群地址创建consumer
    // LocationStrategies.PreferConsistent：持续的在所有Executor之间分配分区
    // ConsumerStrategies：选择如何在Driver和Executor上创建和配置Kafka Consumer
    // ConsumerStrategies.Subscribe：订阅一系列主题

    /**
     *  <artifactId>spark-streaming-kafka-0-10_2.11</artifactId> 导包的时候会自带 工具包 KafkaUtils
     *  使用这个工具包创建一个流，相当于是吧kafka变成一个流。
     */
    def getKafkaStream(topic: String,ssc:StreamingContext): InputDStream[ConsumerRecord[String,String]]={
        // TODO  这个ssc就是 DauApp类里面new出来的一个 StreamingContext， 第二个参数PreferConsistent算是标准配置  第三个参数： 订阅模式， 需要传递主题名！还有kafkaParam -- 包括kafka的地址，序列化的编码方式等等，其实就是kafka的消费者配置
        val dStream = KafkaUtils.createDirectStream[String,String](ssc, LocationStrategies.PreferConsistent,ConsumerStrategies.Subscribe[String,String](Array(topic),kafkaParam))
        dStream
    }
}

