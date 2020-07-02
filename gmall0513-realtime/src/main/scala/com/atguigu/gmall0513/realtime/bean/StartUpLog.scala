package com.atguigu.gmall0513.realtime.bean

// 需要把 {"area":"guangdong","uid":"85","os":"andriod","ch":"huawei","appid":"gmall2019","mid":"mid_170","type":"startup","vs":"1.1.1","ts":1609788151703}
// 封装成下面定义的那种结构

// 注意最后三个字段，这三个并不是原始数据中就有的   ts是后来加上去的时间戳
// 现在还需要将ts 进一步转化形成  ‘天：logDate’ 以及 ‘小时：logHour’ 的字段
// 这就需要在DauApp 中
case class StartUpLog(mid:String,
                      uid:String,
                      appid:String,
                      area:String,
                      os:String,
                      ch:String,
                      logType:String,
                      vs:String,
                      var logDate:String,
                      var logHour:String,
                      var ts:Long
                     ){

}
