package com.qingcheng.timer;

import com.qingcheng.dao.SeckillGoodsMapper;
import com.qingcheng.pojo.seckill.SeckillGood;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tk.mybatis.mapper.entity.Example;

import java.util.Date;
import java.util.List;
import java.util.Set;

@Component
public class SeckillGoodsPushTask {

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private SeckillGoodsMapper seckillGoodsMapper;

    @Scheduled(cron = "0/30 * * * * ?")
    public void loadGoods(){
        //1.查询所有时间区间
        List<Date> dateMenus = DateUtil.getDateMenus();
        //2.循环时间区间，查询每个时间区间的秒杀商品
        for (Date startTime:dateMenus){
            //2.1商品必须通过审核
            Example example = new Example(SeckillGood.class);
            Example.Criteria criteria = example.createCriteria();
            criteria.andEqualTo("status","1");
            //2.2库存>0
            criteria.andGreaterThan("stockCount","0");
            //2.3秒杀开始时间>=当前循环时间区间的开始时间
            criteria.andGreaterThanOrEqualTo("startTime",startTime);
            //2.4秒杀结束时间<当前循环时间区间的开始时间+2小时
            criteria.andLessThan("endTime",DateUtil.addDateHour(startTime,2));
            //过滤Redis中已存在的该区间的秒杀商品
            Set keys = redisTemplate.boundHashOps("SeckillGoods_" + DateUtil.date2Str(startTime)).keys();
            if (keys!=null&&keys.size()>0){
                criteria.andNotIn("id",keys);
            }
            //2.5执行查询
            List<SeckillGood> seckillGoods = seckillGoodsMapper.selectByExample(example);
            //3.将秒杀商品存入到Redis缓存

            for (SeckillGood seckillGood:seckillGoods){
                redisTemplate.boundHashOps("SeckillGoods_"+DateUtil.date2Str(startTime)).put(seckillGood.getId(),seckillGood);
                //商品数据队列存储,防止高并发超卖
                Long[] ids = pushIds(seckillGood.getStockCount(), seckillGood.getId());
                redisTemplate.boundListOps("SeckillGoodsCountList_"+seckillGood.getId()).leftPushAll(ids);
                //自增计数器
                redisTemplate.boundHashOps("SeckillGoodsCount").increment(seckillGood.getId(),seckillGood.getStockCount());

            }

        }

    }

    public Long[] pushIds(int len,Long id){
        Long[] ids = new Long[len];
        for (int i = 0; i <ids.length ; i++) {
            ids[i]=id;
        }
        return ids;
    }
}
