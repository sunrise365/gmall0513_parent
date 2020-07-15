package com.atguigu.gmall0513.publisher.service;

import java.util.List;
import java.util.Map;

public interface PublisherService {

    public Long getDauTotal(String date);

    // 取mapper中的数据结构，转成自己想要的结构
    public Map<String,Long> getDauTotalHours(String date);


    // 总金额
    public Double getOrderAmount(String date);

    // 求分时的金额数据
    public Map<String,Double> getOrderAmountHour(String date);

    // 灵活查询获取数据
    public Map getSaleDetailMap(String date,String keyword,int pageStart,int pageSize);

}
