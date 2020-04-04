package com.qingcheng.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.qingcheng.dao.SeckillGoodsMapper;
import com.qingcheng.dao.SeckillOrderMapper;
import com.qingcheng.pojo.seckill.SeckillGood;
import com.qingcheng.pojo.seckill.SeckillOrder;
import com.qingcheng.service.seckill.SeckillOrderService;
import com.qingcheng.task.MultiThreadingCreateOrder;
import com.qingcheng.util.IdWorker;
import com.qingcheng.util.SeckillStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Date;

@Service
public class SeckillOrderServiceImpl implements SeckillOrderService {

    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private SeckillGoodsMapper seckillGoodsMapper;
    @Autowired
    private IdWorker idWorker;
    @Autowired
    private MultiThreadingCreateOrder multiThreadingCreateOrder;
    @Autowired
    private SeckillOrderMapper seckillOrderMapper;

    @Override
    public SeckillOrder getByUserName(String username) {
        return (SeckillOrder) redisTemplate.boundHashOps("SeckillOrder").get(username);
    }

    @Override
    public void updateStatus(String outtradeno, String transactionid, String username) {
        //订单数据从Redis数据库查询出来
        SeckillOrder seckillOrder = (SeckillOrder)redisTemplate.boundHashOps("SeckillOrder").get(username);
//修改状态
        seckillOrder.setStatus("1");
//支付时间
        seckillOrder.setPayTime(new Date());
//同步到MySQL中
        seckillOrderMapper.insertSelective(seckillOrder);
//清空Redis缓存
        redisTemplate.boundHashOps("SeckillOrder").delete(username);
//清空用户排队数据
        redisTemplate.boundHashOps("UserQueueCount").delete(username);
//删除抢购状态信息
        redisTemplate.boundHashOps("UserQueueStatus").delete(username);
    }

    @Override
    public Boolean add(Long id, String time, String username) {
        //判断是否有库存
        Long size = redisTemplate.boundListOps("SeckillGoodsCountList_" + id).size();
        if(size==null || size<=0){
//101:没有库存
            throw new RuntimeException("101");
        }

        //递增，判断是否排队
        Long userQueueCount = redisTemplate.boundHashOps("UserQueueCount").increment(username, 1);
        if(userQueueCount>1){
//100：表示有重复抢单
            throw new RuntimeException("100");
        }


        //排队信息封装
        SeckillStatus seckillStatus = new SeckillStatus(username, new Date(),1, id,time);
//将秒杀抢单信息存入到Redis中,这里采用List方式存储,List本身是一个队列
        redisTemplate.boundListOps("SeckillOrderQueue").leftPush(seckillStatus);
        //存储排队信息
        redisTemplate.boundHashOps("UserQueueStatus").put(username,seckillStatus);
//多线程操作
        multiThreadingCreateOrder.createOrder();

        return true;
    }

    @Override
    public SeckillStatus queryStatus(String username) {
        return (SeckillStatus) redisTemplate.boundHashOps("UserQueueStatus").get(username);
    }
}
