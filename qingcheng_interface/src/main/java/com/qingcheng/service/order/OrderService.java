package com.qingcheng.service.order;
import com.qingcheng.entity.PageResult;
import com.qingcheng.pojo.order.Order;
import com.qingcheng.pojo.order.Orders;

import java.util.*;

/**
 * order业务逻辑层
 */
public interface OrderService {


    public List<Order> findAll();


    public PageResult<Order> findPage(int page, int size);


    public List<Order> findList(Map<String,Object> searchMap);


    public PageResult<Order> findPage(Map<String,Object> searchMap,int page, int size);


    public Order findById(String id);

    public Map<String,Object> add(Order order);

    public void update(Order order);

    public void delete(String id);

    public Orders findOrdersById(String id);

    public void batchSend(List<Order> orders);

    public void orderTimeOutLogic();


    /**
     * 修改订单状态
     * @param orderId
     * @param transactionId
     */
    public void updatePayStatus(String orderId,String transactionId );

}
