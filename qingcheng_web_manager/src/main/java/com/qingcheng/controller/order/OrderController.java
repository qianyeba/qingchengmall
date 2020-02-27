package com.qingcheng.controller.order;

import com.alibaba.dubbo.config.annotation.Reference;
import com.qingcheng.entity.PageResult;
import com.qingcheng.entity.Result;
import com.qingcheng.pojo.order.Order;
import com.qingcheng.pojo.order.Orders;
import com.qingcheng.service.order.OrderService;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/order")
public class OrderController {

    @Reference
    private OrderService orderService;

    @GetMapping("/findAll")
    public List<Order> findAll(){
        return orderService.findAll();
    }

    @GetMapping("/findPage")
    public PageResult<Order> findPage(int page, int size){
        return orderService.findPage(page, size);
    }

    @PostMapping("/findList")
    public List<Order> findList(@RequestBody Map<String,Object> searchMap){
        return orderService.findList(searchMap);
    }

    @PostMapping("/findPage")
    public PageResult<Order> findPage(@RequestBody Map<String,Object> searchMap,int page, int size){
        return  orderService.findPage(searchMap,page,size);
    }

    @GetMapping("/findById")
    public Order findById(String id){
        return orderService.findById(id);
    }


    @PostMapping("/add")
    public Result add(@RequestBody Order order){
        orderService.add(order);
        return new Result();
    }

    @PostMapping("/update")
    public Result update(@RequestBody Order order){
        orderService.update(order);
        return new Result();
    }

    @GetMapping("/delete")
    public Result delete(String id){
        orderService.delete(id);
        return new Result();
    }

    @GetMapping("/findOrdersById")
    public Orders findGoodsById(String id){
        return orderService.findOrdersById(id);
    }

    @PostMapping("/batchSend")
    public Result batchSend(@RequestBody List<Order> orders){
        orderService.batchSend(orders);
        return new Result();
    }

//    5.1 合并订单
//    需求：页面上选择“主订单号” ，填写“从订单号” ，传递给后端，后端将两个订单合并为一
//            个订单
//    GET /order/ merge.do
//    参数 orderId1 主订单号 orderId2 从订单号
//    实现思路：
//    后端接收两个订单号，将“从订单”的金额等信息合并到“主订单”，“从订单”的订单明细也
//    归属于主订单。
//    合并操作完成后，对“从订单”添加删除标记（逻辑删除）
//    记录订单日志
//5.2 拆分订单
//    需求：页面上选择要拆分的订单明细的商品数量，后端将此订单拆分为两个订单
//    POST /order/split.do 参数：
//            [{
//    id:1,
//    num:10   
//    },
//    {
//    id:2,
//    num:5   
//    }]
//    实现思路：
//    后端接收List 循环，判断要拆分的数量是否大于明细数量，如果通过验证，将拆分的数
//    量产生新的订单。
}
