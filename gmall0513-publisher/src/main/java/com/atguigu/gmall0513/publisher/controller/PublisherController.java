package com.atguigu.gmall0513.publisher.controller;


import com.alibaba.fastjson.JSON;
import com.atguigu.gmall0513.publisher.bean.Option;
import com.atguigu.gmall0513.publisher.bean.Stat;
import com.atguigu.gmall0513.publisher.service.PublisherService;
import com.atguigu.gmall0513.publisher.service.impl.PublisherServiceImpl;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.lang.time.DateUtils;
import org.apache.ibatis.annotations.Param;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;


@RestController // 因为没有页面，只需要返回数据就行
public class PublisherController {

    @Autowired
    PublisherService publisherService;


    // TODO     总数  访问路径	http://publisher:8070/realtime-total?date=2020-07-04    publisher是映射，需要在host中填写相应的映射关系
    @GetMapping("realtime-total")
    public String getRealtimeTotal(@RequestParam("date") String dateString) {
        Long dauTotal = publisherService.getDauTotal(dateString);

        // TODO     总数  需要的数据格式	[{"id":"dau","name":"新增日活","value":1200},{"id":"new_mid","name":"新增设备","value":233} ]
        // 组装成json字符串, 最好就组合成一个java对象，然后把这个java对象使用一些工具（FastJson之类的）转成json，
        // 观察这个数据结构，思考为什么会封装成这样的json串

        List<Map> totalList = new ArrayList<>();
        HashMap dauMap = new HashMap();

        dauMap.put("id", "dau");
        dauMap.put("name", "新增日活");
        dauMap.put("value", dauTotal);

        totalList.add(dauMap);


        HashMap midMap = new HashMap();

        midMap.put("id", "new_mid");
        midMap.put("name", "新增设备");
        midMap.put("value", 323);

        totalList.add(midMap);

        //订单交易总额
        Double orderAmount = publisherService.getOrderAmount(dateString);
        HashMap orderAmountMap = new HashMap();

        orderAmountMap.put("id", "order_amount");
        orderAmountMap.put("name", "新增交易额");
        orderAmountMap.put("value", orderAmount);

        totalList.add(orderAmountMap);

        return JSON.toJSONString(totalList);
    }


    // TODO 分时统计（注意： 这个需要两天的数据，写今天的日期，也会把昨天的数据也给你）
    // 需要返回的数据结构： {"yesterday":{"11":383,"12":123,"17":88,"19":200 },"today":{"12":38,"13":1233,"17":123,"19":688 }}
    @GetMapping("realtime-hour")
    public String getRealtimeHour(@RequestParam("id") String id, @RequestParam("date") String dateString) {
        // 根据id来决定 需要什么图的数据
        if (id.equals("dau")) {
            // 接口地址：http://publisher:8070/realtime-hour?id=dau&date=2020-07-04
            // 通过service层写的逻辑，根据mapper查询数据，转换数据结构以后，可以得到的数据结构类型：
            // {"11":383,"12":123,"17":88,"19":200}
            Map<String, Long> dauTotalHoursTD = publisherService.getDauTotalHours(dateString);
            String yesterday = getYesterday(dateString);
            Map<String, Long> dauTotalHoursYD = publisherService.getDauTotalHours(yesterday);
            // 将两天的数据组合成一个json串
            HashMap<String, Map> hourMap = new HashMap<>();
            hourMap.put("today", dauTotalHoursTD);
            hourMap.put("yesterday", dauTotalHoursYD);
            // 返回json字符串
            return JSON.toJSONString(hourMap);
        } else if ("order_amount".equals(id)) {
            // TODO 访问路径： http://publisher:8070/realtime-hour?id=order_amount&date=2020-07-04
            Map<String, Double> orderAmountHoursTD = publisherService.getOrderAmountHour(dateString);
            String yesterday = getYesterday(dateString);
            Map<String, Double> orderAmountHoursYD = publisherService.getOrderAmountHour(yesterday);
            // 将两天的数据组合成一个json串
            HashMap<String, Map> hourMap = new HashMap<>();
            hourMap.put("today", orderAmountHoursTD);
            hourMap.put("yesterday", orderAmountHoursYD);
            return JSON.toJSONString(hourMap);
        }

        return null;
    }


    /**
     * 这是一个根据今天的日期求昨天的日期的方法
     *
     * @param today
     * @return
     */
    private String getYesterday(String today) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");

        try {
            Date todayDate = simpleDateFormat.parse(today);
            Date yesterdayDate = DateUtils.addDays(todayDate, -1);
            String yesterday = simpleDateFormat.format(yesterdayDate);
            return yesterday;
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }


    // 接口： http://localhost:8070/sale_detail?date=2019-04-01&&startpage=1&size=5&keyword=手机小米
    @GetMapping("sale_detail")
    public String getSaleDetail(@RequestParam("date") String date,
                                @RequestParam("startpage") int startpage,
                                @RequestParam("size") int size,
                                @RequestParam("keyword") String keyword) {
        Map saleMap = publisherService.getSaleDetailMap(date, keyword, startpage, size);
        Long total = (Long) saleMap.get("total");
        Map ageMap = (Map) saleMap.get("ageMap");
        Map genderMap = (Map) saleMap.get("genderMap");
        List<Map> saleDetailList = (List) saleMap.get("list");

        // 性别的饼图数据
        //  genderMap 整理成为  OptionGroup
        Long femaleCount = (Long) genderMap.get("F");
        Long maleCount = (Long) genderMap.get("M");

        double femaleRate = Math.round(femaleCount * 1000D / total) / 10D;
        double maleRate = Math.round(maleCount * 1000D / total) / 10D;

        List<Option> genderOptions = new ArrayList<>();
        genderOptions.add(new Option("男", maleRate));
        genderOptions.add(new Option("女", femaleRate));
        Stat genderStat = new Stat("性别占比", genderOptions);


        //
        Long age_20Count = 0L;
        Long age20_30Count = 0L;
        Long age30_Count = 0L;

        for (Object o : ageMap.entrySet()) {
            Map.Entry entry = (Map.Entry) o;
            String agekey = (String) entry.getKey();
            int age = Integer.parseInt(agekey);
            Long ageCount = (Long) entry.getValue();
            if (age < 20) {
                age_20Count += ageCount;
            } else if (age >= 20 && age < 30) {
                age20_30Count += ageCount;
            } else {
                age30_Count += ageCount;
            }
        }

        Double age_20rate = 0D;
        Double age20_30rate = 0D;
        Double age30_rate = 0D;

        age_20rate = Math.round(age_20Count * 1000D / total) / 10D;
        age20_30rate = Math.round(age20_30Count * 1000D / total) / 10D;
        age30_rate = Math.round(age30_Count * 1000D / total) / 10D;
        List<Option> ageOptions = new ArrayList<>();
        ageOptions.add(new Option("20岁以下", age_20rate));
        ageOptions.add(new Option("20岁到30岁", age20_30rate));
        ageOptions.add(new Option("30岁以上", age30_rate));
        Stat ageStat = new Stat("用户年龄占比", ageOptions);

        // 饼图列表
        List<Stat> StatList = new ArrayList<>();
        StatList.add(genderStat);
        StatList.add(ageStat);

        Map resultMap = new HashMap();

        resultMap.put("total", total);
        resultMap.put("stat", StatList);
        resultMap.put("detail", saleDetailList);

        return JSON.toJSONString(resultMap);
    }
}
