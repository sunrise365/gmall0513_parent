package com.atguigu.gmall0513.publisher.service.impl;

import com.atguigu.gmall0513.publisher.mapper.DauMapper;
import com.atguigu.gmall0513.publisher.mapper.OrderMapper;
import com.atguigu.gmall0513.publisher.service.PublisherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

// 一定要注意，这里不要忘记了写@Service这个注解， 要不然无法实现这个接口，这个是spring来帮你实现接口的，所以需要加上这个注解
@Service
public class PublisherServiceImpl implements PublisherService {

    @Autowired
    OrderMapper orderMapper;


    @Autowired
    DauMapper dauMapper;
    // 这个需要实现的方法其实在业务逻辑中已经写了，就是mapper中的方法，所以 需要在这里声明一个mapper，并实例化(Autowired),Mybatise就会根据配置文件生成实现类，将结果注入其中！！
    @Override
    public Long getDauTotal(String date) {
        return dauMapper.selectDauTotal(date);
    }

    /**
     * 现在就是把从数据库中取出的数据结构，转换成自己想要的数据结构类型
     * 取出来封装的数据类型：[{"LH":"11","CT":489},{"LH":"12","CT":48},{"LH":"13","CT":326}……]
     * 这里转换成的数据格式： {"11":383,"12":123,"17":88,"19":200 }
     *
     * 想要的类型：         {"yesterday":{"11":383,"12":123,"17":88,"19":200 },"today":{"12":38,"13":1233,"17":123,"19":688 }}
     */
    // 这里已经得到的数据类型： {"11":383,"12":123,"17":88,"19":200 }
    @Override
    public Map<String, Long> getDauTotalHours(String date) {
        List<Map> dauListMap = dauMapper.selectDauTotalHours(date);
        HashMap<String, Long> dauMap = new HashMap<>();
        for (Map map : dauListMap) {
            String lh = (String) map.get("LH");
            Long ct = (Long) map.get("CT");
            dauMap.put(lh,ct);
        }
        return dauMap;
    }

    // 总金额 的 实现
    @Override
    public Double getOrderAmount(String date) {
        return orderMapper.selectOrderAmount(date);
    }

    // 求分时的金额数据 的 实现
    @Override
    public Map<String, Double> getOrderAmountHour(String date) {
        /**
         * 现在就是把从数据库中取出的数据结构，转换成自己想要的数据结构类型
         * 取出来封装的数据类型：[{"C_HOUR":"11","AMOUNT":489.0},{"C_HOUR":"16","AMOUNT":89.0},{"C_HOUR":"19","AMOUNT":9.0}……]
         * 这里转换成的数据格式： {"11":489.0,"16":89.0,"19":9.0 ....}
         *
         * 想要的类型：         {"yesterday":{"11":383,"12":123,"17":88,"19":200 },"today":{"12":38,"13":1233,"17":123,"19":688 }}
         */

        List<Map> mapList = orderMapper.selectOrderAmountHour(date);
        HashMap<String, Double> hourMap = new HashMap<>();
        for (Map map : mapList) {
            hourMap.put((String) map.get("C_HOUR"),(Double) map.get("AMOUNT"));
        }
        return hourMap;
    }

}
