package com.atguigu.gmall0513.realtime.app

import com.atguigu.gmall0513.common.constants.GmallConstant
import com.atguigu.gmall0513.realtime.util.MyKafkaUtil
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.spark.streaming.dstream.InputDStream
import org.apache.spark.streaming.{Seconds, StreamingContext}
import org.apache.spark.{SparkConf, SparkContext}
import com.alibaba.fastjson.JSON
import java.text.SimpleDateFormat
import java.util.Date
import com.atguigu.gmall0513.realtime.bean.EventInfo
import java.util
import scala.util.control.Breaks._
import com.atguigu.gmall0513.realtime.bean.CouponAlertInfo
import com.atguigu.gmall0513.realtime.util.MyEsUtil
import org.apache.spark.streaming.dstream.DStream



object CouponAlertApp {
    def main(args: Array[String]): Unit = {
        val sparkConf: SparkConf = new SparkConf().setAppName("coupon_alert_app").setMaster("local[*]")
        val ssc: StreamingContext = new StreamingContext(sparkConf, Seconds(5))
        val inputDstream: InputDStream[ConsumerRecord[String, String]] = MyKafkaUtil.getKafkaStream(GmallConstant.KAFKA_EVENT, ssc)

        // 1. 调整结构  record ==> case class
        val eventDstream: DStream[EventInfo] = inputDstream.map { record =>
            val jsonString: String = record.value()
            val eventInfo: EventInfo = JSON.parseObject(jsonString, classOf[EventInfo])
            // 先把ts变成日期 的 字符串
            val formator: SimpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH")
            val dateHour: String = formator.format(new Date(eventInfo.ts))
            val dateHourArr: Array[String] = dateHour.split(" ")
            eventInfo.logDate = dateHourArr(0)
            eventInfo.logHour = dateHourArr(1)
            eventInfo
        }

        // 2. 开窗, 可以在开窗范围内的数据进行调整结构，以外的不管了
        // 窗口大小决定了你统计的数据范围，滑动步长决定频次，
        // 最后注意这个步长需要考虑自己的计算能力，至少能够达到计算流程需要的时间，如果整个计算流程需要15秒，结果开5分钟的窗口会有问题
        val eventWindowsDstream: DStream[EventInfo] = eventDstream.window(Seconds(300), Seconds(10))


        // 3. 分组 --> 先转成k，v结构
        val eventGroupbyMidDstream: DStream[(String, Iterable[EventInfo])] = eventDstream.map(eventInfo => (eventInfo.mid, eventInfo)).groupByKey()

        // 4. 筛选
        //    a 三次几以上领取优惠券   //在组内根据mid的事件集合进行 过滤筛选
        //    b 用不同账号
        //    c  在过程中没有浏览商品

        // 这个方法的目的： 1 判断出来是否达到预警的要求 2 如果达到要求组织预警的信息
        val alertDstream: DStream[(Boolean, CouponAlertInfo)] = eventGroupbyMidDstream.map { case (mid, eventInfoItr) =>

            // 先设置一个标记，默认是true， 在循环的时候如果这个人的条件达到了预警就设置false，否则就让他正常走下去
            var ifAlert: Boolean = true;
            // 登陆过的uid
            val uidSet: util.HashSet[String] = new util.HashSet[String]()
            // 领取的商品id
            val itemIdSet: util.HashSet[String] = new util.HashSet[String]()
            // 做过哪些行为
            val eventList: util.ArrayList[String] = new util.ArrayList[String]()

            breakable(
                for (eventInfo: EventInfo <- eventInfoItr) {
                    if (eventInfo.evid == "coupon") {
                        uidSet.add(eventInfo.uid)
                        itemIdSet.add(eventInfo.itemid)
                    }
                    eventList.add(eventInfo.evid)
                    if (eventInfo.evid == "clickItem") {
                        ifAlert = false;
                        break
                    }
                }
            )

            if (uidSet.size() < 3) {
                ifAlert = false;
            }

            (ifAlert, CouponAlertInfo(mid, uidSet, itemIdSet, eventList, System.currentTimeMillis()))
        }

        // 测试一下，打印已经筛选的数据，是否合格
        // 数据格式：(false,CouponAlertInfo(mid_44,[],[],[addCart, addCart, addComment, addComment, addFavor],1594176826661))
        /*alertDstream.foreachRDD{rdd =>
            println(rdd.collect().mkString("\n"))
        }*/

        // 过滤,
        val filterAlertDstream: DStream[(Boolean, CouponAlertInfo)] = alertDstream.filter(_._1)
        //  转换结构 （ifAlert,alertInfo）=>（mid_minu , alertInfo）     构造一个id，这个id是 mid+分钟 一起构成的
        val alertInfoWithIdDstream: DStream[(String, CouponAlertInfo)] = filterAlertDstream.map { case (ifAlert, alertInfo) =>
            val uniKey = alertInfo.mid + "_" + alertInfo.ts / 1000 / 60 // alertInfo.ts 是毫秒数
            (uniKey, alertInfo)
        }


        // 5. 存储->es   提前建好index  和 mapping ， mapping就是结构
        alertInfoWithIdDstream.foreachRDD{rdd =>  //在这个流里面每个结算结果提取成一个rdd
                //把rdd变成分区，每一个分区里的数据可以得到一个集合
            rdd.foreachPartition{ alertInfoItr =>
                val alertList: List[(String, CouponAlertInfo)] = alertInfoItr.toList

                MyEsUtil.insertBulk(alertList,GmallConstant.ES_INDEX_ALERT,GmallConstant.ES_DEFAULT_TYPE)

            }
        }

        ssc.start()
        ssc.awaitTermination()
    }

}
