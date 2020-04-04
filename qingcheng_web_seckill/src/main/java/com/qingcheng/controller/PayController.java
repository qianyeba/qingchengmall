package com.qingcheng.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.qingcheng.pojo.seckill.SeckillOrder;
import com.qingcheng.service.order.WxPayService;
import com.qingcheng.service.seckill.SeckillOrderService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

@RestController
@RequestMapping("/pay")
public class PayController {
    @Reference
    private WxPayService weixinPayService;
    @Reference
    private SeckillOrderService seckillOrderService;
    /**
     * 生成二维码
     * @return
     */
    @GetMapping("/createNative")
    public Map createNative(){
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        SeckillOrder seckillOrder = seckillOrderService.getByUserName(username);
        if(seckillOrder!=null){
//校验该订单是否时当前用户的订单
            if(username.equals(seckillOrder.getUserId())){
                int money = (int) ((seckillOrder.getMoney().doubleValue())*100);
                return weixinPayService.createNative(
                        seckillOrder.getId().toString(),
                        money,
                        "http://qingcheng.cross.echosite.cn/pay/notify.do");
            }else{
                return null;
            }
        }else{
            return null;
        }
    }

    /***
     * 查询订单状态
     * @param orderId
     * @return
     */
    @GetMapping("/queryPayStatus")
    public Map<String, String> queryPayStatus(String orderId){
        Map<String,String> resultMap = weixinPayService.queryPayStatus(orderId);
        if(resultMap.get("return_code").equalsIgnoreCase("success") && resultMap.get("result_code").equalsIgnoreCase("success")){
//获取支付状态
            String result = resultMap.get("trade_state");
            if(result.equalsIgnoreCase("success")){
                String username = SecurityContextHolder.getContext().getAuthentication().getName();
//支付成功,修改订单状态
                seckillOrderService.updateStatus(resultMap.get("out_trade_no"),resultMap.get("transaction _id"),username);
            }
        }
        return resultMap;
    }

    @RequestMapping("/notify")
    public void notifyLogic(HttpServletRequest request){
        System.out.println("支付成功回调。。。。");
        try {
            InputStream inputStream = (InputStream)request.getInputStream();
            ByteArrayOutputStream outputStream=new ByteArrayOutputStream();

            byte[] buffer=new byte[1024];
            int len=0;
            while( ( len= inputStream.read(buffer) )!=-1   ){
                outputStream.write( buffer,0,len );
            }
            outputStream.close();
            inputStream.close();
            String result=new String( outputStream.toByteArray(),"utf-8" );
            System.out.println(result);
            Map<String, String> map = WXPayUtil.xmlToMap(result);
            String username=map.get("attach");
            seckillOrderService.updateStatus(map.get("out_trade_no"),map.get("transaction _id"),username));
//            weixinPayService.notifyLogic(result);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}