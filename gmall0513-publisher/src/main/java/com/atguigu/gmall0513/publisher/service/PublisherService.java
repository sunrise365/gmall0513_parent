package com.atguigu.gmall0513.publisher.service;

import java.util.Map;

public interface PublisherService {

    public Long getDauTotal(String date);

    // 取mapper中的数据结构，转成自己想要的结构
    public Map<String,Long> getDauTotalHours(String date);
}
