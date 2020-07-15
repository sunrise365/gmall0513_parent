package com.atguigu.gmall0513.canal.app;

import com.alibaba.otter.canal.client.CanalConnector;
import com.alibaba.otter.canal.client.CanalConnectors;
import com.alibaba.otter.canal.protocol.CanalEntry;
import com.alibaba.otter.canal.protocol.Message;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

import java.net.InetSocketAddress;
import java.util.List;

public class CanalApp {
    public static void main(String[] args) {
        // TODO 1. 连接canal的服务器 (得到canal的连接器)
        // 注意，最后用户名和密码不是canal，我们配置的canal是sever端连接mysql时候的用户名和密码，现在要的是client连接server的，我们没有配置，默认空值就行
        CanalConnector canalConnector = CanalConnectors.newSingleConnector(new InetSocketAddress("hadoop102", 11111),
                "example", "", "");

        // TODO 2. 抓取数据
        while (true) {
            canalConnector.connect();
            // 在订阅的时候，如果不写参数或者写 库名.*，默认是整个库所有的数据，如果写参数，可以指定拉取的数据（表）
            canalConnector.subscribe("gmall0513.*");
            // 一个message就是一次抓取，一次抓取可以抓取 多个sql的 执行结果集！
            // 也就是说如果写1，可不是一条数据，而是执行一条sql的一个单元的结果集，可以是一条数据，也可以是很多条数据，因为我们在mysql的配置文件中配置的格式是row！！
            Message message = canalConnector.get(100);

            // 在抓取的时候可能没有数据，entry就相当于是一个sql单元
            if (message.getEntries().size() == 0) {
                System.out.println("没有数据，休息5秒");
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else {
                // TODO 3. 抓取数据后，提取数据
                // 一个entry就代表一个sql执行的结果集
                for (CanalEntry.Entry entry : message.getEntries()) {

                    // 可以做一个优化， 因为mysql有很多写操作我们并不关心，比如开始关闭事务，心跳之类的，我们只需要操作数据的写操作
                    if (entry.getEntryType() == CanalEntry.EntryType.ROWDATA) {
                        // getStoreValue(): 需要的核心业务数据
                        // getStoreValue()方法返回的结果是ByteString，这个是一个序列化以后的结果，没法直接用，
                        ByteString storeValue = entry.getStoreValue();
                        // 需要使用它自带的一个工具 CanalEntry.RowChange进行反序列化
                        CanalEntry.RowChange rowChange = null;
                        try {
                            // 需要使用它自带的一个工具 Canal.RowChange进行反序列化
                            rowChange = CanalEntry.RowChange.parseFrom(storeValue);
                        } catch (InvalidProtocolBufferException e) {
                            e.printStackTrace();
                        }
                        // 获取行数据列表（注意，rowDatasList 就是每一行的数据）
                        List<CanalEntry.RowData> rowDatasList = rowChange.getRowDatasList();

                        // 获取第二个参数，tableName：
                        String tableName = entry.getHeader().getTableName();
                        CanalHandler canalHandler = new CanalHandler(rowChange.getEventType(), tableName, rowDatasList);

                        // TODO 4. 处理业务逻辑，发送kafka 到对应的topic
                        // 业务逻辑的处理
                        canalHandler.handle();
                    }
                }
            }

        }

    }
}
