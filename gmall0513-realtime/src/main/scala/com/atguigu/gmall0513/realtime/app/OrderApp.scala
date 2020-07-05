package com.atguigu.gmall0513.realtime.app
import org.apache.spark.SparkConf
import com.atguigu.gmall0513.common.constants.GmallConstant
import com.atguigu.gmall0513.realtime.util.MyKafkaUtil
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.spark.streaming.dstream.InputDStream
import org.apache.spark.streaming.{Seconds, StreamingContext}
import com.alibaba.fastjson.JSON
import com.atguigu.gmall0513.realtime.bean.OrderInfo
import org.apache.spark.streaming.dstream.DStream
import org.apache.phoenix.spark._

object OrderApp {
    def main(args: Array[String]): Unit = {
        val conf: SparkConf = new SparkConf().setAppName("order_app").setMaster("local[*]")
        val ssc: StreamingContext = new StreamingContext(conf, Seconds(5))

        val inputDstream: InputDStream[ConsumerRecord[String, String]] = MyKafkaUtil.getKafkaStream(GmallConstant.KAFKA_ORDER, ssc)

        // TODO 变换结构     Record => case class
        val orderDstream: DStream[OrderInfo] = inputDstream.map { record =>
            val jsonString: String = record.value()
            val orderInfo: OrderInfo = JSON.parseObject(jsonString, classOf[OrderInfo])
            // createTime 的格式： 2020-07-04 22:18:27 根据这个格式切出来 date和hour
            val createTimeArr: Array[String] = orderInfo.create_time.split(" ")
            orderInfo.create_date = createTimeArr(0)
            val hourArr: Array[String] = createTimeArr(1).split(":")
            orderInfo.create_hour = hourArr(0)
            // 电话的格式是： 138****1393  只留前三位和后四位，其余都是*
            val tel3_8: (String, String) = orderInfo.consignee_tel.splitAt(3)
            val front3: String = tel3_8._1
            val back4: String = tel3_8._2.splitAt(4)._2
            orderInfo.consignee_tel = front3 + "****" + back4
            orderInfo
        }

        // TODO 转换格式并且清洗完了数据以后， 写数据！！ 现在是往hbase中写数据， hbase+Phoenix是用来做分析的数据库
        //      小知识： redis没有工具，所以需要一条条的放，但是 Phoenix和spark是有整合的，可以直接放，别忘了加隐式转换！！！！
        orderDstream.foreachRDD{rdd =>
            // 这里是需要建表的，用来接收数据！
            import org.apache.hadoop.conf.Configuration
            rdd.saveToPhoenix("gmall0513_order_info",
                Seq("ID","PROVINCE_ID", "CONSIGNEE", "ORDER_COMMENT", "CONSIGNEE_TEL", "ORDER_STATUS", "PAYMENT_WAY", "USER_ID","IMG_URL", "TOTAL_AMOUNT", "EXPIRE_TIME", "DELIVERY_ADDRESS", "CREATE_TIME","OPERATE_TIME","TRACKING_NO","PARENT_ORDER_ID","OUT_TRADE_NO", "TRADE_BODY", "CREATE_DATE", "CREATE_HOUR"),
                new Configuration,
                Some("hadoop102,hadoop103,hadoop103:2181"))
        }

        ssc.start()
        ssc.awaitTermination()
    }
}
