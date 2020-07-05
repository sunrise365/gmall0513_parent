package com.atguigu.gmall0513.realtime.bean

/**
 * 这里面大部分的数据和原始数据一样，如果有需要，也可以直接筛选掉一部分
 *
 * 和原来一样，新增两个字段：create_date、 create_hour 需要在转换封装成case class的时候添加上去
 *      这个要求，在原来的字段中就有时间，create_time， 所以可以利用这个create_time， 分解成 天 和 小时
 */
case class OrderInfo(
                            id: String,
                            province_id: String,
                            consignee: String,
                            order_comment: String,
                            var consignee_tel: String,
                            order_status: String,
                            payment_way: String,
                            user_id: String,
                            img_url: String,
                            total_amount: Double,
                            expire_time: String,
                            delivery_address: String,
                            create_time: String,
                            operate_time: String,
                            tracking_no: String,
                            parent_order_id: String,
                            out_trade_no: String,
                            trade_body: String,
                            var create_date: String,
                            var create_hour: String

                    )

