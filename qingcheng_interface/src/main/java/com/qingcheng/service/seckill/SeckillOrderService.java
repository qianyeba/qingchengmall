package com.qingcheng.service.seckill;

import com.qingcheng.pojo.seckill.SeckillOrder;
import com.qingcheng.util.SeckillStatus;

/****
 * @Author:shenkunlin
 * @Date:2019/5/8 14:19
 * @Description:shenkunlin写的代码
 *****/
public interface SeckillOrderService {


    /***
     * 根据用户名查询用户的未支付秒杀订单
     * @param username
     */
    SeckillOrder getByUserName(String username);


    /****
     * 修改订单状态
     * @param username
     * @param transactionid
     * @param outtradeno
     */
    void updateStatus(String outtradeno, String transactionid, String username);



    /***
     * 添加秒杀订单
     * @param id:商品ID
     * @param time:商品秒杀开始时间
     * @param username:用户登录名
     * @return
     */
    Boolean add(Long id, String time, String username);

    /****
     * 查询订单状态
     * @param username
     */
     SeckillStatus queryStatus(String username);
}
