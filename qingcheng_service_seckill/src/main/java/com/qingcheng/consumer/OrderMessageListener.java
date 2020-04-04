package com.qingcheng.consumer;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.qingcheng.dao.SeckillGoodsMapper;
import com.qingcheng.dao.SeckillOrderMapper;
import com.qingcheng.pojo.seckill.SeckillGood;
import com.qingcheng.pojo.seckill.SeckillOrder;
import com.qingcheng.service.order.WxPayService;
import com.qingcheng.util.SeckillStatus;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Map;

public class OrderMessageListener implements MessageListener {
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private SeckillOrderMapper seckillOrderMapper;
    @Autowired
    private SeckillGoodsMapper seckillGoodsMapper;
    @Reference
    private WxPayService weixinPayService;
    @Override
    public void onMessage(Message message) {
        try {
            String content = new String(message.getBody());
            System.out.println("监听到消息"+content);
            //将消息转换成SeckillStatus
            SeckillStatus seckillStatus = JSON.parseObject(content,SeckillStatus.class);
            //订单处理以及回滚库存处理
            rollbackOrder(seckillStatus);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
    /***
     * 订单处理以及回滚库存处理
     * @param seckillStatus
     */
    public void rollbackOrder(SeckillStatus seckillStatus) throws Exception {
//获取Redis中订单信息
        String username = seckillStatus.getUsername();
        SeckillOrder seckillOrder = (SeckillOrder)redisTemplate.boundHashOps("SeckillOrder").get(username);
//如果Redis中有订单信息，说明用户未支付
        if(seckillOrder!=null){
//关闭支付
            Map<String,String> closeResult = weixinPayService.closePay(seckillStatus.getOrderId());
            if(closeResult.get("return_code").equalsIgnoreCase("success") && closeResult.get("result_code").equalsIgnoreCase("success") ){
//删除订单
                redisTemplate.boundHashOps("SeckillOrder").delete(username);
//回滚库存
//1)从Redis中获取该商品
                SeckillGood seckillGoods = (SeckillGood) redisTemplate.boundHashOps("SeckillGoods_"+seckillStatus.getTime()).get(seckillStatus.getGoodsId());
//2)如果Redis中没有，则从数据库中加载
                if(seckillGoods==null){
                    seckillGoods = seckillGoodsMapper.selectByPrimaryKey(seckillStatus.getGoodsId());
                }
//3)数量+1 (递增数量+1，队列数量+1)
                Long surplusCount = redisTemplate.boundHashOps("SeckillGoodsCount").increment(seckillStatus.getGoodsId(), 1);
                seckillGoods.setStockCount(surplusCount.intValue());
                redisTemplate.boundListOps("SeckillGoodsCountList_" + seckillStatus.getGoodsId()).leftPush(seckillStatus.getGoodsId());
//4)数据同步到Redis中
                redisTemplate.boundHashOps("SeckillGoods_"+seckillStatus.getTime()).put(seckillStatus.getGoodsId(),seckillGoods);
//清理排队标示
                redisTemplate.boundHashOps("UserQueueCount").delete(seckillStatus.getUsername());
//清理抢单标示
                redisTemplate.boundHashOps("UserQueueStatus").delete(seckillStatus.getUsername());
            }
        }
    }

}
