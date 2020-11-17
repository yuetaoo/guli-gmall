package com.atguigu.gmall.pms.controller;

import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.pms.entity.SkuAttrValueEntity;
import com.atguigu.gmall.pms.entity.vo.SaleAttrValueVo;
import com.atguigu.gmall.pms.service.SkuAttrValueService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * sku销售属性&值
 *
 * @author Tao
 * @email fengge@atguigu.com
 * @date 2020-10-28 18:51:28
 */
@Api(tags = "sku销售属性&值 管理")
@RestController
@RequestMapping("pms/skuattrvalue")
public class SkuAttrValueController {

    @Autowired
    private SkuAttrValueService skuAttrValueService;

    @GetMapping("sku/mapping/{spuId}")
    public ResponseVo<String> querySaleAttrValuesMappingSkuId(@PathVariable("spuId")Long spuId){
        String s = skuAttrValueService.querySaleAttrValuesMappingSkuId(spuId);
        return ResponseVo.ok(s);
    }

    @GetMapping("spu/{spuId}")
    public ResponseVo<List<SaleAttrValueVo>> querySaleAttrValuesByspuId(@PathVariable("spuId") Long spuId){
        List<SaleAttrValueVo> saleAttrValueVos = skuAttrValueService.querySaleAttrValuesByspuId(spuId);
        return ResponseVo.ok(saleAttrValueVos);
    }

    @GetMapping("sale/attr/{skuId}")
    public ResponseVo<List<SkuAttrValueEntity>> querySaleBySkuId(@PathVariable("skuId")Long skuId){
        List<SkuAttrValueEntity> attrValueEntities = this.skuAttrValueService.querySaleBySkuId(skuId);
        return ResponseVo.ok(attrValueEntities);
    }

    @GetMapping("search/attr/{skuId}")
    public ResponseVo<List<SkuAttrValueEntity>> querySearchAttrValueBySkuidAndCid(
            @PathVariable("skuId") Long skuId,
            @RequestParam("cid") Long cid ){
        List<SkuAttrValueEntity> list = this.skuAttrValueService.querySearchAttrValueBySkuidAndCid(skuId, cid);
        return ResponseVo.ok(list);
    }

    /**
     * 列表
     */
    @GetMapping
    @ApiOperation("分页查询")
    public ResponseVo<PageResultVo> querySkuAttrValueByPage(PageParamVo paramVo){
        PageResultVo pageResultVo = skuAttrValueService.queryPage(paramVo);

        return ResponseVo.ok(pageResultVo);
    }


    /**
     * 信息
     */
    @GetMapping("{id}")
    @ApiOperation("详情查询")
    public ResponseVo<SkuAttrValueEntity> querySkuAttrValueById(@PathVariable("id") Long id){
		SkuAttrValueEntity skuAttrValue = skuAttrValueService.getById(id);

        return ResponseVo.ok(skuAttrValue);
    }

    /**
     * 保存
     */
    @PostMapping
    @ApiOperation("保存")
    public ResponseVo<Object> save(@RequestBody SkuAttrValueEntity skuAttrValue){
		skuAttrValueService.save(skuAttrValue);

        return ResponseVo.ok();
    }

    /**
     * 修改
     */
    @PostMapping("/update")
    @ApiOperation("修改")
    public ResponseVo update(@RequestBody SkuAttrValueEntity skuAttrValue){
		skuAttrValueService.updateById(skuAttrValue);

        return ResponseVo.ok();
    }

    /**
     * 删除
     */
    @PostMapping("/delete")
    @ApiOperation("删除")
    public ResponseVo delete(@RequestBody List<Long> ids){
		skuAttrValueService.removeByIds(ids);

        return ResponseVo.ok();
    }

}
