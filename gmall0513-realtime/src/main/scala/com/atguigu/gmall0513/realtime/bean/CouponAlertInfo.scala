package com.atguigu.gmall0513.realtime.bean

/**
 * 这个就是如果设置预警的话，需要得到的信息
 */
case class CouponAlertInfo(mid:String,
                           uids:java.util.HashSet[String],
                           itemIds:java.util.HashSet[String],
                           events:java.util.List[String],
                           ts:Long)  {

}

