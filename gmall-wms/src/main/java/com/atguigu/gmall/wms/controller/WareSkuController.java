package com.atguigu.gmall.wms.controller;

import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import com.atguigu.gmall.wms.entity.vo.SkuLockVo;
import com.atguigu.gmall.wms.service.WareSkuService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 商品库存
 * @author Tao
 * @email fengge@atguigu.com
 * @date 2020-10-29 10:52:35
 */
@Api(tags = "商品库存 管理")
@RestController
@RequestMapping("wms/waresku")
public class WareSkuController {

    @Autowired
    private WareSkuService wareSkuService;

    @PostMapping("check/lock/{orderToken}")
    public ResponseVo<List<SkuLockVo>> checkAndLock(@RequestBody List<SkuLockVo> lockVos, @PathVariable("orderToken")String orderToken ){
        List<SkuLockVo> lockVoList = wareSkuService.checkAndLock(orderToken, lockVos);
        return ResponseVo.ok(lockVoList);
    }

    @ApiOperation("根据skuId查询库存信息")
    @GetMapping("sku/{skuId}")
    public ResponseVo<List<WareSkuEntity>> queryWareSkuBySkuId(@PathVariable("skuId") Long skuId){
        List<WareSkuEntity> list = wareSkuService.list(new QueryWrapper<WareSkuEntity>().eq("sku_id", skuId));
        return ResponseVo.ok(list);
    }

    /**
     * 列表
     */
    @GetMapping
    @ApiOperation("分页查询")
    public ResponseVo<PageResultVo> queryWareSkuByPage(PageParamVo paramVo){
        PageResultVo pageResultVo = wareSkuService.queryPage(paramVo);

        return ResponseVo.ok(pageResultVo);
    }

    /**
     * 信息
     */
    @GetMapping("{id}")
    @ApiOperation("详情查询")
    public ResponseVo<WareSkuEntity> queryWareSkuById(@PathVariable("id") Long id){
		WareSkuEntity wareSku = wareSkuService.getById(id);

        return ResponseVo.ok(wareSku);
    }

    /**
     * 保存
     */
    @PostMapping
    @ApiOperation("保存")
    public ResponseVo<Object> save(@RequestBody WareSkuEntity wareSku){
		wareSkuService.save(wareSku);

        return ResponseVo.ok();
    }

    /**
     * 修改
     */
    @PostMapping("/update")
    @ApiOperation("修改")
    public ResponseVo update(@RequestBody WareSkuEntity wareSku){
		wareSkuService.updateById(wareSku);

        return ResponseVo.ok();
    }

    /**
     * 删除
     */
    @PostMapping("/delete")
    @ApiOperation("删除")
    public ResponseVo delete(@RequestBody List<Long> ids){
		wareSkuService.removeByIds(ids);
        return ResponseVo.ok();
    }

}
