package com.qingcheng.task;

import com.alibaba.fastjson.JSON;
import com.qingcheng.dao.SeckillGoodsMapper;
import com.qingcheng.pojo.seckill.SeckillGood;
import com.qingcheng.pojo.seckill.SeckillOrder;
import com.qingcheng.util.IdWorker;
import com.qingcheng.util.SeckillStatus;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class MultiThreadingCreateOrder {
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private SeckillGoodsMapper seckillGoodsMapper;
    @Autowired
    private IdWorker idWorker;
    @Async
    public void createOrder(){
        try {
            //从队列中获取排队信息
            SeckillStatus seckillStatus = (SeckillStatus)redisTemplate.boundListOps("SeckillOrderQueue").rightPop();

            //时间区间
            String time = seckillStatus.getTime();
            //用户登录名
            String username=seckillStatus.getUsername();
            //用户抢购商品
            Long id = seckillStatus.getGoodsId();

            //获取队列中商品id
            Object sid = redisTemplate.boundListOps("SeckillGoodsCountList_" + id).rightPop();
            //售罄
            if (sid==null){
                //清理排队信息
                clearQueue(seckillStatus);
                return;
            }

//获取商品数据
            SeckillGood goods = (SeckillGood) redisTemplate.boundHashOps("SeckillGoods_" + time).get(id);
//如果没有库存，则直接抛出异常
            if(goods==null || goods.getStockCount()<=0){
                throw new RuntimeException("已售罄!");
            }
//如果有库存，则创建秒杀商品订单
            SeckillOrder seckillOrder = new SeckillOrder();
            seckillOrder.setId(idWorker.nextId());
            seckillOrder.setSeckillId(id);
            seckillOrder.setMoney(goods.getCostPrice());
            seckillOrder.setUserId(username);
            seckillOrder.setSellerId(goods.getSellerId());
            seckillOrder.setCreateTime(new Date());
            seckillOrder.setStatus("0");
//将秒杀订单存入到Redis中
            redisTemplate.boundHashOps("SeckillOrder").put(username,seckillOrder);

            //商品库存-1
            Long surplusCount = redisTemplate.boundHashOps("SeckillGoodsCount").increment(id, -1);//商品数量递减
            goods.setStockCount(surplusCount.intValue()); //根据计数器统计


            //判断当前商品是否还有库存
            if(goods.getStockCount()<=0){
//并且将商品数据同步到MySQL中
                seckillGoodsMapper.updateByPrimaryKeySelective(goods);
//如果没有库存,则清空Redis缓存中该商品
                redisTemplate.boundHashOps("SeckillGoods_" + time).delete(id);
            }else{
//如果有库存，则直数据重置到Reids中
                redisTemplate.boundHashOps("SeckillGoods_" + time).put(id,goods);
            }
            //抢单成功，更新抢单状态,排队->等待支付
            seckillStatus.setStatus(2);
            seckillStatus.setOrderId(seckillOrder.getId());
            seckillStatus.setMoney(seckillOrder.getMoney().floatValue());

            redisTemplate.boundHashOps("UserQueueStatus").put(username,seckillStatus);

            //发送MQ消息
            sendDelayMessage(seckillStatus);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    /***
     * 清理用户排队信息
     * @param seckillStatus
     */
    public void clearQueue(SeckillStatus seckillStatus){
//清理排队标示
        redisTemplate.boundHashOps("UserQueueCount").delete(seckillStatus.getUsername());
//清理抢单标示
        redisTemplate.boundHashOps("UserQueueStatus").delete(seckillStatus.getUsername());
    }

    @Autowired
    private RabbitTemplate rabbitTemplate;
    /***
     * 延时消息发送
     * @param seckillStatus
     */
    public void sendDelayMessage(SeckillStatus seckillStatus){
        rabbitTemplate.convertAndSend(
                "exchange.delay.order.begin",
                "delay",
                JSON.toJSONString(seckillStatus),
                new MessagePostProcessor() {
                    @Override
                    public Message postProcessMessage(Message message) throws AmqpException {
                        //消息有效期30分钟
                        //message.getMessageProperties().setExpiration(String.valueOf(1800000));
                        message.getMessageProperties().setExpiration(String.valueOf(10000));
                        return message;
                    }
                });
    }
}
