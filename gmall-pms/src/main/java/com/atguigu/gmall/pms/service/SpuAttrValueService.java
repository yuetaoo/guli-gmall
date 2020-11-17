package com.atguigu.gmall.pms.service;

import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.pms.entity.SpuAttrValueEntity;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * spu属性值
 *
 * @author Tao
 * @email fengge@atguigu.com
 * @date 2020-10-28 18:51:28
 */
public interface SpuAttrValueService extends IService<SpuAttrValueEntity> {

    PageResultVo queryPage(PageParamVo paramVo);

    List<SpuAttrValueEntity> querySearchSpuAttrValueBySpuIdAndCid(Long spuId, Long cid);

}

