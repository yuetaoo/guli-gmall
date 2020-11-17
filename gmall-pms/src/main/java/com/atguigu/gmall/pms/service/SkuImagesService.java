package com.atguigu.gmall.pms.service;

import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.pms.entity.SkuImagesEntity;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * sku图片
 *
 * @author Tao
 * @email fengge@atguigu.com
 * @date 2020-10-28 18:51:28
 */
public interface SkuImagesService extends IService<SkuImagesEntity> {

    PageResultVo queryPage(PageParamVo paramVo);

    List<SkuImagesEntity> queryImagesBySkuId(Long skuId);
}

