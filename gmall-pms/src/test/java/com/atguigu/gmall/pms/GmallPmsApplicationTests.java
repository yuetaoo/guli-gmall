package com.atguigu.gmall.pms;

import com.atguigu.gmall.pms.mapper.SkuAttrValueMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class GmallPmsApplicationTests {

    @Autowired
    private SkuAttrValueMapper skuAttrValueMapper;

    @Test
    void querySaleAttrValuesMappingSkuId() {
        System.out.println(skuAttrValueMapper.querySaleAttrValuesMappingSkuIdMapper(7l));
    }

    @Test
    void contextLoads() {
        System.out.println(1111111111);
    }

}
