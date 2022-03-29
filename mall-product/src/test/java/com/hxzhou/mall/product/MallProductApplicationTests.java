package com.hxzhou.mall.product;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.hxzhou.mall.product.entity.BrandEntity;
import com.hxzhou.mall.product.service.BrandService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
class MallProductApplicationTests {

    @Autowired
    BrandService brandService;

    @Test
    public void contextLoads() {
        BrandEntity brandEntity = new BrandEntity();

//        brandEntity.setName("华为");
//        brandService.save(brandEntity);
//
//        System.out.println("保存成功！");

//        brandEntity.setBrandId(1L);
//        brandEntity.setDescript("中华有为");
//        brandService.updateById(brandEntity);

        List<BrandEntity> list = brandService.list(new QueryWrapper<BrandEntity>().eq("brand_id", 1L));
        list.forEach((item)->{
            System.out.println(item);
        });


    }

}
