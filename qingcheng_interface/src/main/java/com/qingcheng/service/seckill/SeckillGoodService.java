package com.qingcheng.service.seckill;

import com.qingcheng.pojo.seckill.SeckillGood;

import java.util.List;

/***
 *
 * @Author:shenkunlin
 * @Description:itheima
 * @date: 2019/5/7 15:21
 *
 ****/
public interface SeckillGoodService {

    /***
     * 获取指定时间对应的秒杀商品列表
     * @param key
     */
    List<SeckillGood> list(String key);


    /****
     * 根据ID查询商品详情
     * @param time:时间区间
     * @param id:商品ID
     */
    SeckillGood one(String time, Long id);

}
