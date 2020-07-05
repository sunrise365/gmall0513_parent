package com.atguigu.gmall0513.publisher.mapper;

import java.util.List;
import java.util.Map;

public interface OrderMapper {

    // 求总金额
    public Double selectOrderAmount(String date);


    // 求分时的金额数据
    public List<Map> selectOrderAmountHour(String date);
}
