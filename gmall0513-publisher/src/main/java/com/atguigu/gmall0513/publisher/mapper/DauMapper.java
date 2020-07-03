package com.atguigu.gmall0513.publisher.mapper;

import java.util.List;
import java.util.Map;

public interface DauMapper {

    /**
     * 查询某日用户活跃总数
     *  定义完需要查询数据的方法以后，需要在resource里面建立一个 mapper的目录，里面的文件的名称也叫作DauMapper.xml， 这两个之间会有一定的关系
     *  一般来说接口定义方法，实现类中定义具体怎么去做，但是mybatis中不需要实现类。
     *
     *  那么实现类是怎么产生的呢？ 实现类是由mybatis通过加载定义在resource中的同名.xml 的这个配置文件，从内部生成一个，实现这个接口的实现类
     */
    // TODO     总数  数据格式	[{"id":"dau","name":"新增日活","value":1200},{"id":"new_mid","name":"新增设备","value":233} ]
    public Long selectDauTotal(String date);

    /**
     * 查询某日用户活跃数的分时值
     * 返回值：如果数据比较多最好封装成一个bean里面更好，这里就直接封装在一个list里面的map结构中
     */
    // TODO 想要的数据结构（从下面的结构过渡，转成现在的结构）：
    //  {"yesterday":{"11":383,"12":123,"17":88,"19":200 },"today":{"12":38,"13":1233,"17":123,"19":688 }}
    public List<Map> selectDauTotalHours(String date);
    // xml配置文件中封装的数据转成json的结构：
    // [{"LH":"11","CT":489},{"LH":"12","CT":48},{"LH":"13","CT":326}……]

}
