package com.atguigu.gmall.pms.api;

import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.pms.entity.vo.GroupVo;
import com.atguigu.gmall.pms.entity.vo.SaleAttrValueVo;
import org.springframework.web.bind.annotation.*;

import java.util.List;

public interface GmallPmsApi {

    @GetMapping("pms/attrgroup/withattrvalues")
    public ResponseVo<List<GroupVo>> queryGroupsBySpuIdAndCid(
            @RequestParam("spuId")Long spuId,
            @RequestParam("skuId")Long skuId,
            @RequestParam("cid")Long cid );

    @PostMapping("pms/spu/json")
    public ResponseVo<List<SpuEntity>> querySpuByPageJson(@RequestBody PageParamVo paramVo);

    @GetMapping("pms/spu/{id}")
    public ResponseVo<SpuEntity> querySpuById(@PathVariable("id") Long id);

    @GetMapping("pms/spudesc/{spuId}")
    public ResponseVo<SpuDescEntity> querySpuDescById(@PathVariable("spuId") Long spuId);

    @GetMapping("pms/skuattrvalue/spu/{spuId}")
    public ResponseVo<List<SaleAttrValueVo>> querySaleAttrValuesByspuId(@PathVariable("spuId") Long spuId);

    @GetMapping("pms/skuattrvalue/sku/mapping/{spuId}")
    public ResponseVo<String> querySaleAttrValuesMappingSkuId(@PathVariable("spuId")Long spuId);

    @GetMapping("pms/sku/spu/{spuId}")
    public ResponseVo<List<SkuEntity>> querySkuBySpuId(@PathVariable("spuId") Long spuId);

    @GetMapping("pms/sku/{id}")
    public ResponseVo<SkuEntity> querySkuById(@PathVariable("id") Long id);

    @GetMapping("pms/skuimages/sku/{skuId}")
    public ResponseVo<List<SkuImagesEntity>> queryImagesBySkuId(@PathVariable("skuId")Long skuId);

    @GetMapping("pms/skuattrvalue/sale/attr/{skuId}")
    public ResponseVo<List<SkuAttrValueEntity>> querySaleBySkuId(@PathVariable("skuId")Long skuId);

    @GetMapping("pms/brand/{id}")
    public ResponseVo<BrandEntity> queryBrandById(@PathVariable("id") Long id);

    @GetMapping("pms/category/{id}")
    public ResponseVo<CategoryEntity> queryCategoryById(@PathVariable("id") Long id);

    @GetMapping("pms/category/parent/{parentId}")
    public ResponseVo<List<CategoryEntity>> queryCategory(@PathVariable("parentId") Long parentId );

    @GetMapping("pms/category/parent/sub/{parentId}")
    public ResponseVo<List<CategoryEntity>> quueryLv2CategorySubsByPid(@PathVariable("parentId") Long pId);

    @GetMapping("pms/category/all/{cid}")
    public ResponseVo<List<CategoryEntity>> queryAllCategoryByCid3(@PathVariable("cid") Long cid);

    @GetMapping("pms/skuattrvalue/search/attr/{skuId}")
    public ResponseVo<List<SkuAttrValueEntity>> querySearchAttrValueBySkuidAndCid(
            @PathVariable("skuId") Long skuId,
            @RequestParam("cid") Long cid );

    @GetMapping("pms/spuattrvalue/search/attr/{spuId}")
    public ResponseVo<List<SpuAttrValueEntity>> querySearchSpuAttrValueBySpuIdAndCid(
            @PathVariable("spuId") Long spuId,
            @RequestParam("cid") Long cid
    );
}
