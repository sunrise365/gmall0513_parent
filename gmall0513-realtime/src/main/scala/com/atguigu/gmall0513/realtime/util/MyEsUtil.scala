package com.atguigu.gmall0513.realtime.util

import java.util
import java.util.Objects

import io.searchbox.client.{JestClient, JestClientFactory}
import io.searchbox.client.config.HttpClientConfig
import io.searchbox.core.{Bulk, BulkResult, Index}
import collection.JavaConversions._

object MyEsUtil {
    private val ES_HOST = "http://hadoop102"
    private val ES_HTTP_PORT = 9200
    private var factory: JestClientFactory = null

    /**
     * 获取客户端
     *
     * @return jestclient
     */
    def getClient: JestClient = {
        if (factory == null) build()
        // 从工厂中取连接
        factory.getObject
    }

    /**
     * 关闭客户端
     */
    def close(client: JestClient): Unit = {
        if (client != null) try
            client.shutdownClient()
        catch {
            case e: Exception =>
                e.printStackTrace()
        }
    }


    /**
     * 建立连接, (造工厂，配置工厂的参数)
     */
    private def build(): Unit = {
        factory = new JestClientFactory
        factory.setHttpClientConfig(new HttpClientConfig.Builder(ES_HOST + ":" + ES_HTTP_PORT).multiThreaded(true)
                .maxTotalConnection(20) //连接总数
                .connTimeout(10000).readTimeout(10000).build)

    }

    // 测试学习用的
    def insert(source:Any): Unit = {
        val jest: JestClient = getClient
        // 构造一个数据的对象
        // val stud4 = Stud("zhangsan", "male")

        // 获取一个index，这个index作为动词就是插入的意思， 代表依次插入动作，index操作在jest中就是一个新增的操作，
        //  往哪个库里面插入，插入的数据是什么？通过定义index完成， Builder中类型虽然是any，但是应该插入一个对象
        val index: Index = new Index.Builder(source).index("gmall2019_stud").`type`("stud").id("stu123").build()
        // 执行插入的动作，execute中放的是一个动作，里面放什么动作就执行什么操作
        jest.execute(index)
    }


    // 批量操作
    // 注意，如果id不想自动生成，需要自己定义的话，可以在参数的时候传入id + 数据，这样传入的参数算是一个tuple
    def insertBulk(sourceList:List[(String,Any)],indexName:String,typeName:String): Unit = {
        val jest: JestClient = getClient
        // 把Bulk提出来做构造器
        val bulkBuilder: Bulk.Builder = new Bulk.Builder()
        // 应为是批次操作，所以把inde和type都提取出来，设置成默认的，不需要在循环中每次都做
        bulkBuilder.defaultIndex(indexName).defaultType(typeName)

        for ((id,source) <- sourceList) {
            // 在循环中把index插入的动作全都放在bulk中，这样bulk中就有很多的动作，然后build以后放在execute中执行bulk，批量操作，减少连接
            val index: Index = new Index.Builder(source).id(id).build()
            bulkBuilder.addAction(index)
        }

        //val bulk: Bulk = new Bulk.Builder().build()

        val bulk: Bulk = bulkBuilder.build()

        // 执行插入的动作,  得到插入后的结果，并打印
        val items: util.List[BulkResult#BulkResultItem] = jest.execute(bulk).getItems
        println(s" 保存= ${items.size()} 条数据")
    }


    def main(args: Array[String]): Unit = {
        insert()
    }


    case class Stud(name: String, nickname: String)

}

