package com.atguigu.gmall.wms.service;

import com.atguigu.gmall.wms.entity.vo.SkuLockVo;
import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.wms.entity.WareSkuEntity;

import java.util.List;

/**
 * 商品库存
 *
 * @author Tao
 * @email fengge@atguigu.com
 * @date 2020-10-29 10:52:35
 */
public interface WareSkuService extends IService<WareSkuEntity> {

    PageResultVo queryPage(PageParamVo paramVo);

    List<SkuLockVo> checkAndLock(String orderToken, List<SkuLockVo> lockVos);
}

