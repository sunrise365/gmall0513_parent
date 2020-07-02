package com.atguigu.gmall0513.realtime.app
import com.atguigu.gmall0513.realtime.util.MyKafkaUtil
import org.apache.spark.SparkConf
import org.apache.spark.streaming.{Seconds, StreamingContext}
import java.text.SimpleDateFormat
import java.util
import java.util.Date

import com.atguigu.gmall0513.common.constants.GmallConstant
import com.atguigu.gmall0513.realtime.bean.StartUpLog
import com.atguigu.gmall0513.realtime.util.RedisUtil
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.spark.broadcast.Broadcast
import org.apache.spark.streaming.dstream.{DStream, InputDStream}
import redis.clients.jedis.Jedis

import java.text.SimpleDateFormat
import java.util.Date

import com.alibaba.fastjson.JSON
import com.atguigu.gmall0513.realtime.bean.StartUpLog

import org.apache.spark.rdd.RDD
import org.apache.phoenix.spark._
import org.apache.hadoop.conf.Configuration

/**
 * 1  消费kafka
 * 2  整理一下数据结构  string json  =>  case class
 * 3  根据清单进行过滤
 * 4  把用户访问清单保存到redis中
 *
 * 5  保存真正数据库(hbase)
 */
object DauApp {
    def main(args: Array[String]): Unit = {

        // TODO 导入spark 配置环境，消费kafka的数据
        // driver通过SparkContext对象来访问 Spark, SparkContext对象相当于一个到 Spark 集群的连接
        val sparkConf = new SparkConf().setAppName("dau_app").setMaster("local[*]")
        // 流式编程 StreamingContext ， 第二个参数是时间--多长时间一个批次，按批次取数据
        val ssc = new StreamingContext(sparkConf,Seconds(5))
        // 然后需要消费kafka，需要消费kafka的一个工具类，加一个包放在util中，叫做MyKafkaUtil
        val inputDstream: InputDStream[ConsumerRecord[String, String]] = MyKafkaUtil.getKafkaStream(GmallConstant.KAFKA_STARTUP, ssc)

        // TODO 数据类型： {"area":"beijing","uid":"172","os":"andriod","ch":"website","appid":"gmall2019","mid":"mid_141","type":"startup","vs":"1.1.3","ts":1610149281827}
//        inputDstream.foreachRDD(rdd =>
//            println(rdd.map(_.value()).collect().mkString("\n"))
//        )

        // TODO 对应：  2  整理一下数据结构  string json  =>  case class
        // 转换格式  同时补充两个时间字段
        val startUplogDstream: DStream[StartUpLog] = inputDstream.map{record =>

            // 先把 类型为 [ConsumerRecord[String, String] 的数据转换成 json字符串，然后定义 case class再进行转换
            val jsonString: String = record.value()
            val startUpLog: StartUpLog = JSON.parseObject(jsonString, classOf[StartUpLog])
            // 先把ts变成日期 的 字符串
            val formator: SimpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH")
            val dateHour: String = formator.format(new Date(startUpLog.ts))
            val dateHourArr: Array[String] = dateHour.split(" ")
            startUpLog.logDate = dateHourArr(0)
            startUpLog.logHour = dateHourArr(1)
            startUpLog
        }


        // TODO 添加一个缓存， 在节点中暂时存储数据，如果下一阶段没有处理完数据，后面来的数据可以放在这个节点的缓存中，等处理完了再从缓存中读取数据
        startUplogDstream.cache()


        // TODO  3  根据清单进行过滤---(注意：如果无法过滤数据，我这里是因为Windows的时间和Linux的时间不是同一天，导致获取的时间戳不一样，导致从redis查询数据的时候都是空的，所以没有过滤掉任何数据)
        // TODO 过滤的最终版代码： 使用transform解决第二版的问题，并且也使用广播解决了redis连接消耗的问题
                // 这个外面的代码是在driver中执行的
        val filteredDstream: DStream[StartUpLog] = startUplogDstream.transform { rdd =>
            println("过滤前：" + rdd.count())
            // 这里的代码也是在driver中执行的，但是这里是spark流中，所以会每5秒（批次）执行一次
            val jedis: Jedis = RedisUtil.getJedisClient // driver
            val dateString: String = new SimpleDateFormat("yyyy-MM-dd").format(new Date())
            val dauKey = "dau:" + dateString
            //println(dateString)
            val dauMidSet: util.Set[String] = jedis.smembers(dauKey)
            val dauMidBC: Broadcast[util.Set[String]] = ssc.sparkContext.broadcast(dauMidSet)

            val filteredRDD: RDD[StartUpLog] = rdd.filter { startuplog => // executor
                val dauMidSet: util.Set[String] = dauMidBC.value
                val flag: Boolean = dauMidSet.contains(startuplog.mid)
                !flag
            }
            println("过滤后：" + filteredRDD.count())
            filteredRDD
        }


        // TODO 过滤的第二版代码
        // 在启动run以后会执行这段代码，但是之后不会执行了，之后循环不停执行的代码必须写在流里面
        // 造成的结果就是今天只会取一个批次，让driver发送给executor！然后就再也不会发了
        // 这个时候就想到使用流的另外一个算子： transform
        /*val jedis: Jedis = RedisUtil.getJedisClient     // driver
        val dateString: String = new SimpleDateFormat("yyyy-MM-dd").format(new Date())
        val dauKey = "dau:" + dateString
        val dauMidSet: util.Set[String] = jedis.smembers(dauKey)
        // 定义广播变量：
        val dauMidBC: Broadcast[util.Set[String]] = ssc.sparkContext.broadcast(dauMidSet)

        startUplogDstream.filter { startuplog => // executor
            val dauMidSet: util.Set[String] = dauMidBC.value
            val flag: Boolean = dauMidSet.contains(startuplog.mid)
            !flag
        }*/

        //  TODO 过滤的第一版原始代码，
        // 最原始的写法，可以优化的，这里主要还是连接消耗的问题，如何做呢？
        // 思考： spark是一种微批次的数据流，我们自己设置了每5秒更新一次数据，也就是说5秒内数据是没有变化的
        //      既然5秒内数据是固定的，那可以先把数据从redis中查出来，每个executor中放一份，这样再查询的数据的时候不需要过网线，自己查询自己内存中的数据就行了
        //      可以使用广播变量！driver发送广播变量，driver把redis数据取出来，每5秒给executor发送一次，这样就不需要进行executor的数据与redis进行交互了
        // 总结一下思路就是 spark拿到数据以后需要去重，去重是根据redis中的mid进行去重的，所以spark每拿到一次数据都需要和redis进行交互，判断，决定是否留下来，现在不需要了，使用广播变量即可
        /*startUplogDstream.filter { startuplog =>
            import java.lang
            val jedis = RedisUtil.getJedisClient //driver
            val dauKey = "dau:" + startuplog.logDate
            val flag: lang.Boolean = jedis.sismember(dauKey, startuplog.mid)
            !flag
        }*/


        //TODO 还有最后一个问题就是如果在这5秒钟，产生了相同的mid，这时候因为redis缓存中没有这个mid，所以在过滤的时候不会去重
        //简单的说就是同一个批次内有相同的数据无法去重现在，虽然这些数据到redis中因为使用set类型会自动去重，但是我们最终放的是数据库，那个是没有自动去重功能的
        //所以还需要同批次的数据去重！
        //批次内去重 ， 同一批次内，相同mid 只保留第一条 ==> 对相同的mid进行分组，组内进行比较 保留第一条
        val startupDstreamGroupByMid: DStream[(String, Iterable[StartUpLog])] = filteredDstream.map(startUplog => (startUplog.mid, startUplog)).groupByKey()
        val startupRealFilteredDstream: DStream[StartUpLog] = startupDstreamGroupByMid.flatMap {
            case (mid, startupItr) =>
            val top1List: List[StartUpLog] = startupItr.toList.sortWith { (startup1, startup2) =>
                startup1.ts < startup2.ts
            }.take(1)
            top1List
        }


        // 按理说，从思路上应该是第三步，先进行过滤在保存， 但是实际上一般都会先保存，打通流程并查看保存文件的格式
        // TODO 4  把用户访问清单保存到redis中
        // 注意： 下面这种方式虽然可以，但是可以优化，因为建立连接非常消耗性能！！我们这里建立连接是放在foreach中的，也就是说遍历一次都会新建立一个连接，用完了再关闭
        // 如何做到， 一次连接多次使用呢？？？？
        import com.atguigu.gmall0513.realtime.util.RedisUtil
        startupRealFilteredDstream.foreachRDD { rdd =>
                // 这个foreachPartition 是每个分区执行一次， startupItr是一个迭代器，代表这个分区中所有的数据，能够迭代这个分区中的所有数据
            rdd.foreachPartition { startupItr =>
                //executor 执行一次
                // val jedis: Jedis = new Jedis("hadoop102", 6379) 不用这个，继续优化，使用redis连接池，封装在redisUtil类中
                val jedis = RedisUtil.getJedisClient //driver
                for (startup <- startupItr) {
                    //println(startup)   // 查看过滤后的数据的时间和mid
                    //executor  反复执行
                    val dauKey = "dau:" + startup.logDate
                    jedis.sadd(dauKey, startup.mid)
                }
                jedis.close()
            }
        }


        // TODO  在这里有可能出问题，因为当流里面的数据过来的时候，这个方法需要将字段一个个进行处理，可能会出现处理不及时的问题，就是一个数据没有保存完后面的数据救过来了，
        //  导致分流现象，解决的办法就是在前面加上一个cache，用来作为缓存
        startupRealFilteredDstream.foreachRDD{rdd =>
            // 注意，第一个参数tableName： 大小写问题     第二个参数是把 列名放在Seq中，这里面的字段需要和流里面的每个对象startup的字段值一一对应（名字无所谓，顺序一定不能错）。
            // 第三个配置是选择hadoop的configuration
            // 第四个参数是scala特有的类型Option，（some或者是null类型），里面放zookeeper的地址, 最后一个参数可以不写
            rdd.saveToPhoenix("GMALL0513_DAU",Seq("MID", "UID", "APPID", "AREA", "OS", "CH", "TYPE", "VS", "LOGDATE", "LOGHOUR", "TS"),new Configuration(),Some("hadoop102,hadoop103,hadoop104:2181"))
        }


        //  开启sparkStreamingContext，并且终端不中断
        ssc.start()
        ssc.awaitTermination()
    }
}





/*
startUplogDstream.foreachRDD { rdd =>
    // 这一行是在driver中执行的，如果要把redis的连接提到这里会报错说没有序列化，这是因为如果程序在jvm就不需要序列化，但是只要数据从jvm中出去就一定要序列化
    // 一般从jvm出去 就两个地方， 一个是磁盘， 一个是走网线 都需要序列化
    rdd.foreach(startUpLog => {
    // 这里是在excutor中执行的
    import redis.clients.jedis.Jedis
    // 保存redis的操作，  所有今天访问过的mid的清单
    // redis  1、type ：set   2.key ：[dau：2019-10-19]     3. value：[mid]
    val jedis: Jedis = new Jedis("hadoop102", 6379)
    // 定义sadd 所需要的key
    val dauKey = "dau:" + startUpLog.logDate
    jedis.sadd(dauKey,startUpLog.mid)
    // 最后别忘记关了， 因为每一次都打开一个连接，每个连接都用完了不关，用一段时间就会连接枯竭
    // 这个连接是redis服务器中有一个连接池，供外部使用的，不关，就会被一直占用，别人就用不了了
    jedis.close()
})
}*/
