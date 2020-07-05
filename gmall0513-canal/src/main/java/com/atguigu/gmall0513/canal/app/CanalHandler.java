package com.atguigu.gmall0513.canal.app;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.otter.canal.protocol.CanalEntry;
import com.atguigu.gmall0513.canal.util.MyKafkaSender;
import com.atguigu.gmall0513.common.constants.GmallConstant;

import java.util.List;

/**
 * 专门进行业务处理的类
 */
public class CanalHandler {
    // 处理数据的时候的三个依据

    // 这个数据发生的是什么操作，（比如update，还是insert）
    CanalEntry.EventType eventType;

    // 这个数据是哪个表的数据
    String tableName;

    // 数据的结果集
    List<CanalEntry.RowData> rowDataList;

    // 构造函数
    public CanalHandler(CanalEntry.EventType eventType, String tableName, List<CanalEntry.RowData> rowDataList) {
        this.eventType = eventType;
        this.tableName = tableName;
        this.rowDataList = rowDataList;
    }

    // TODO 核心业务
    // 处理数据
    public void handle(){
        // 下单的操作， 知道表名，以及相应的操作
        if (tableName.equals("order_info") && eventType==CanalEntry.EventType.INSERT){

            /*// 遍历行集合  （注意： rowDatasList 就是每一行的数据！！）
            for (CanalEntry.RowData rowData : rowDataList) {
                // before是数据发生变化以前的，after是数据变化发生以后的，
                // 如果是insert，那么before就没有，只有after，  如果是update，before、after都有  如果是delete，只有before，没有after

                // 获取列集合
                List<CanalEntry.Column> afterColumnsList = rowData.getAfterColumnsList();
                for (CanalEntry.Column column : afterColumnsList) {
                    String name = column.getName();
                    String value = column.getValue();
                    // ？？根据打印的数据结构可以反推： rowData经过数据变化发生以后  获取的列集column是一个 [字段名,值] 的集合、
                    // 也就是说 rowData 是值， 使用getAfterColumnsList将 key与value合并到一起组成一个新的键值对，并放到一个集合中！
                    // System.out.println(name + ": " + value);
                }
            }*/

            sendRowDataList2Topic(GmallConstant.KAFKA_ORDER);

        }else if (tableName.equals("user_info") && eventType==CanalEntry.EventType.INSERT){
            sendRowDataList2Topic(GmallConstant.KAFKA_USER);
        }
    }


    // todo 继续优化！ if条件判断表名和类型，中间的业务逻辑其实都是一样的，
    //  唯一不同的就是向不同的kafka的topic中发送数据，所以可以把这些代码提取出来，封装成一个方法
    public void sendRowDataList2Topic(String topic){
        // 遍历行集合  （注意： rowDatasList 就是每一行的数据！！）
        for (CanalEntry.RowData rowData : rowDataList) {
            // before是数据发生变化以前的，after是数据变化发生以后的，
            // 如果是insert，那么before就没有，只有after，  如果是update，before、after都有  如果是delete，只有before，没有after

            // 获取列集合
            List<CanalEntry.Column> afterColumnsList = rowData.getAfterColumnsList();
            JSONObject jsonObject = new JSONObject();
            for (CanalEntry.Column column : afterColumnsList) {
                String name = column.getName();
                String value = column.getValue();
                jsonObject.put(name,value);
                // 控制台打印，查询数据
                // System.out.println(name + ": " + value);
            }
            String rowJson = jsonObject.toJSONString();
            //System.out.println(rowJson);          查看封装成的json字符串
            // 自定义一个创建kafka的生产者的工具类，同时封装发送的方法
            MyKafkaSender.send(topic,rowJson);
        }
    }

}
