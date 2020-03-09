package com.qingcheng.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.qingcheng.pojo.goods.Goods;
import com.qingcheng.pojo.goods.Sku;
import com.qingcheng.pojo.goods.Spu;
import com.qingcheng.service.goods.CategoryService;
import com.qingcheng.service.goods.SpuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/item")
public class ItemController {

    @Reference
    private SpuService spuService;

    @Reference
    private CategoryService categoryService;

    @Value("${pagePath}")
    private String pagePath;

    @Autowired
    private TemplateEngine templateEngine;

    @GetMapping("/createPage")
    public void createPage(String spuId){
        //1.查询商品信息
        Goods goods = spuService.findGoodsById(spuId);
        Spu spu = goods.getSpu();
        List<Sku> skuList = goods.getSkuList();
        //查询商品分类
        List<String> categoryList= new ArrayList<>();
        categoryList.add(categoryService.findById(spu.getCategory1Id()).getName());
        categoryList.add(categoryService.findById(spu.getCategory2Id()).getName());
        categoryList.add(categoryService.findById(spu.getCategory3Id()).getName());

        //sku地址列表
        Map<String,String> urlMap = new HashMap<>();
        for (Sku sku:skuList){
            if("1".equals(sku.getStatus())){
                String specJSON = JSON.toJSONString(JSON.parseObject(sku.getSpec()), SerializerFeature.MapSortField);
                urlMap.put(sku.getSpec(),sku.getId()+".html");
            }
        }

        //2.批量生成sku页面
        for (Sku sku:skuList){
             //创建上下文和数据模型
            Context context = new Context();
            Map<String,Object> dataModel = new HashMap<>();
            dataModel.put("spu",spu);
            dataModel.put("sku",sku);
            dataModel.put("categoryList",categoryList);
            dataModel.put("skuImages",sku.getImages().split(","));
            dataModel.put("spuImages",spu.getImages().split(","));
            Map paraItems = JSON.parseObject(spu.getParaItems());//参数列表
            dataModel.put("paraItems",paraItems);
            Map<String,String> specItems = (Map)JSON.parseObject(sku.getSpec());//规格列表
            dataModel.put("specItems",specItems);

            Map<String,List> specMap = (Map)JSON.parseObject(spu.getSpecItems());//规格和规格选项
            for (String key:specMap.keySet()){//循环规格
                List<String> list=specMap.get(key);
                List<Map> mapList = new ArrayList<>();
                for(String value:list){//循环规格选项
                    Map map = new HashMap();
                    map.put("option",value);//规格选项
                    if (specItems.get(key).equals(value)){//如果和当前sku规格相同
                        map.put("checked",true);//是否选中
                    }else {
                        map.put("checked",false);//是否选中
                    }
                    Map<String,String> spec = (Map)JSON.parseObject(sku.getSpec());//当前的sku
                    spec.put(key,value);
                    String specJSON = JSON.toJSONString(spec, SerializerFeature.MapSortField);
                    map.put("url",urlMap.get(specJSON));
                    mapList.add(map);
                }
                specMap.put(key,mapList);
            }
            dataModel.put("specMap",specMap);

            context.setVariables(dataModel);
//            准备文件
            File dir = new File(pagePath);
            if (!dir.exists()){
                dir.mkdirs();
            }
            File dest =new File(dir,sku.getId()+".html");
//            生成页面
            PrintWriter printWriter = null;
            try {
                printWriter = new PrintWriter(dest,"UTF-8");
                templateEngine.process("item",context,printWriter);
                System.out.println("生成页面"+sku.getId()+".html");
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

        }
    }
}
