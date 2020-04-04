package com.qingcheng.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.qingcheng.entity.Result;
import com.qingcheng.service.seckill.SeckillOrderService;
import com.qingcheng.util.SeckillStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/seckill/order")
public class SeckillOrderController {
    @Reference
    private SeckillOrderService seckillOrderService;
    /***
     * URL:/seckill/order/add
     * 秒杀下单
     * @return
     */
    @RequestMapping(value = "/add")
    public Result add(Long id,String time){
        try {
            //获取用户名
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            //如果用户账号为anonymousUser，则表明用户未登录
            if(username.equalsIgnoreCase("anonymousUser")){
            //这里403错误代码表示用户没登录
                return new Result(403,"请先登录！");
            }

            boolean bo=seckillOrderService.add(id,time,username);
            if (bo){
                return new Result(0,"抢单成功！");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return new Result(2,e.getMessage());
        }
        return new Result(1,"秒杀下单失败！");
    }

    @RequestMapping(value = "/query")
    public Result queryStatus(){
//获取用户名
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
//如果用户账号为anonymousUser，则表明用户未登录
        if(username.equalsIgnoreCase("anonymousUser")){
//这里403错误代码表示用户没登录
            return new Result(403,"请先登录！");
        }
//根据用户名查询用户抢购状态
        SeckillStatus seckillStatus = seckillOrderService.queryStatus(username);
        if(seckillStatus!=null){
//获取订单号
            Result result = new Result(seckillStatus.getStatus(),"抢单状态！");
            result.setOther(seckillStatus);
            return result;
        }
        return new Result(404,"没有抢购信息");
    }


}
