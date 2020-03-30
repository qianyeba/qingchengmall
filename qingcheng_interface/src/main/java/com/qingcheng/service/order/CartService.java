package com.qingcheng.service.order;

import java.util.List;
import java.util.Map;

public interface CartService {

    public List<Map<String,Object>> findCartList(String username);

    public void addItem(String name,String skuId,Integer num);

    public boolean updateChecked(String username,String skuId,boolean checked);

    public void deleteCheckedCart(String username);

    public int preferential(String username);

    public List<Map<String,Object>> findNewOrderItemList(String username);
}
