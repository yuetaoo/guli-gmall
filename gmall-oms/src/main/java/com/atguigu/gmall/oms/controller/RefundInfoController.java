package com.atguigu.gmall.oms.controller;

import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.oms.entity.RefundInfoEntity;
import com.atguigu.gmall.oms.service.RefundInfoService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 退款信息
 *
 * @author Tao
 * @email fengge@atguigu.com
 * @date 2020-10-31 11:49:00
 */
@Api(tags = "退款信息 管理")
@RestController
@RequestMapping("oms/refundinfo")
public class RefundInfoController {

    @Autowired
    private RefundInfoService refundInfoService;

    /**
     * 列表
     */
    @GetMapping
    @ApiOperation("分页查询")
    public ResponseVo<PageResultVo> queryRefundInfoByPage(PageParamVo paramVo){
        PageResultVo pageResultVo = refundInfoService.queryPage(paramVo);

        return ResponseVo.ok(pageResultVo);
    }


    /**
     * 信息
     */
    @GetMapping("{id}")
    @ApiOperation("详情查询")
    public ResponseVo<RefundInfoEntity> queryRefundInfoById(@PathVariable("id") Long id){
		RefundInfoEntity refundInfo = refundInfoService.getById(id);

        return ResponseVo.ok(refundInfo);
    }

    /**
     * 保存
     */
    @PostMapping
    @ApiOperation("保存")
    public ResponseVo<Object> save(@RequestBody RefundInfoEntity refundInfo){
		refundInfoService.save(refundInfo);

        return ResponseVo.ok();
    }

    /**
     * 修改
     */
    @PostMapping("/update")
    @ApiOperation("修改")
    public ResponseVo update(@RequestBody RefundInfoEntity refundInfo){
		refundInfoService.updateById(refundInfo);

        return ResponseVo.ok();
    }

    /**
     * 删除
     */
    @PostMapping("/delete")
    @ApiOperation("删除")
    public ResponseVo delete(@RequestBody List<Long> ids){
		refundInfoService.removeByIds(ids);

        return ResponseVo.ok();
    }

}
