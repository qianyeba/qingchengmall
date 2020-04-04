package com.qingcheng.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.qingcheng.pojo.seckill.SeckillGood;
import com.qingcheng.service.seckill.SeckillGoodService;
import com.qingcheng.timer.DateUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.List;

@Service
public class SeckillGoodServiceImpl implements SeckillGoodService {

    @Autowired
    private RedisTemplate redisTemplate;
    public List<SeckillGood> list(String key) {
        return redisTemplate.boundHashOps(key).values();
    }

    public SeckillGood one(String time, Long id) {
        return (SeckillGood) redisTemplate.boundHashOps("SeckillGoods_"+time).get(id);
    }
}
