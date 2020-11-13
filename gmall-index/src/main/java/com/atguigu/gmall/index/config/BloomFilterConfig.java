package com.atguigu.gmall.index.config;

import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.index.feign.GmallPmsClient;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.util.CollectionUtils;

import java.util.List;

@Configuration
public class BloomFilterConfig {

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private GmallPmsClient pmsClient;

    private static final String KEY_PREFIX = "index:cates:[";

    /**
     * 获取布隆过滤器，把所有分类放到过滤器中
     * @return
     */
    @Bean
    public RBloomFilter<String> bloomFilter(){
        RBloomFilter<String> bloomFilter = redissonClient.getBloomFilter("index:cates:bloom");
        //初始化bloomFilter
        bloomFilter.tryInit(10000, 0.03);
        ResponseVo<List<CategoryEntity>> responseVo = pmsClient.queryCategory(0l);
        List<CategoryEntity> categoryEntities = responseVo.getData();
        if(!CollectionUtils.isEmpty(categoryEntities)){
            categoryEntities.forEach((category) -> {
                bloomFilter.add(KEY_PREFIX + category.getId().toString() + "]");
            });
        }
        return bloomFilter;
    }

    @Scheduled //bloomFilter定时更新
    public void testFulshBloom(){
        RBloomFilter<String> bloomFilter = redissonClient.getBloomFilter("index:cates:bloom");
        //初始化bloomFilter
        bloomFilter.tryInit(10000, 0.03);
        ResponseVo<List<CategoryEntity>> responseVo = pmsClient.queryCategory(0l);
        List<CategoryEntity> categoryEntities = responseVo.getData();
        if(!CollectionUtils.isEmpty(categoryEntities)){
            categoryEntities.forEach((category) -> {
                bloomFilter.add(KEY_PREFIX + category.getId().toString() + "]");
            });
        }
    }
}
