package com.qingcheng.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.qingcheng.pojo.seckill.SeckillGood;
import com.qingcheng.service.seckill.SeckillGoodService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/seckill/goods")
public class SeckillGoodController {

    @Reference
    private SeckillGoodService seckillGoodService;

    @GetMapping("/one")
    public SeckillGood one(String time,Long id){
        return seckillGoodService.one(time,id);
    }

    @GetMapping("/list")
    public List<SeckillGood> list(String time){
        return seckillGoodService.list("SeckillGoods_"+time);
    }

    @RequestMapping("/menus")
    public List<Date> loadMenus(){
        return DateUtil.getDateMenus();
    }
}
