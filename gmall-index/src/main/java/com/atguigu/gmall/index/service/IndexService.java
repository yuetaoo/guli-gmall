package com.atguigu.gmall.index.service;

import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.index.feign.GmallPmsClient;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class IndexService {

    @Autowired
    private GmallPmsClient pmsClient;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String KEY_PREFIX = "index:cates:";

    public List<CategoryEntity> queryLv1Categories() {
        ResponseVo<List<CategoryEntity>> categories = pmsClient.queryCategory(0L);
        return categories.getData();
    }

    public List<CategoryEntity> quueryLv2CategoryWithSubsByPid(Long pId){
        //先查询缓存
        String json = redisTemplate.opsForValue().get(KEY_PREFIX + pId);
        if(StringUtils.isNotBlank(json)){
            //命中缓存直接返回
            try {
                return MAPPER.readValue(json, new TypeReference<List<CategoryEntity>>(){});
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }

        //没有命中执行业务远程调用。最后放入缓存
        ResponseVo<List<CategoryEntity>> responseVo = pmsClient.quueryLv2CategorySubsByPid(pId);
        //放入缓存
        String categoryEntities = null;
        try {
            categoryEntities = MAPPER.writeValueAsString(responseVo.getData());
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        this.redisTemplate.opsForValue().set(KEY_PREFIX + pId, categoryEntities);
        return responseVo.getData();
    }
}
