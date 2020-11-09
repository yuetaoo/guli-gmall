package com.atguigu.gmall.oms.controller;

import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.oms.entity.OrderOperateHistoryEntity;
import com.atguigu.gmall.oms.service.OrderOperateHistoryService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 订单操作历史记录
 *
 * @author Tao
 * @email fengge@atguigu.com
 * @date 2020-10-31 11:49:00
 */
@Api(tags = "订单操作历史记录 管理")
@RestController
@RequestMapping("oms/orderoperatehistory")
public class OrderOperateHistoryController {

    @Autowired
    private OrderOperateHistoryService orderOperateHistoryService;

    /**
     * 列表
     */
    @GetMapping
    @ApiOperation("分页查询")
    public ResponseVo<PageResultVo> queryOrderOperateHistoryByPage(PageParamVo paramVo){
        PageResultVo pageResultVo = orderOperateHistoryService.queryPage(paramVo);

        return ResponseVo.ok(pageResultVo);
    }


    /**
     * 信息
     */
    @GetMapping("{id}")
    @ApiOperation("详情查询")
    public ResponseVo<OrderOperateHistoryEntity> queryOrderOperateHistoryById(@PathVariable("id") Long id){
		OrderOperateHistoryEntity orderOperateHistory = orderOperateHistoryService.getById(id);

        return ResponseVo.ok(orderOperateHistory);
    }

    /**
     * 保存
     */
    @PostMapping
    @ApiOperation("保存")
    public ResponseVo<Object> save(@RequestBody OrderOperateHistoryEntity orderOperateHistory){
		orderOperateHistoryService.save(orderOperateHistory);

        return ResponseVo.ok();
    }

    /**
     * 修改
     */
    @PostMapping("/update")
    @ApiOperation("修改")
    public ResponseVo update(@RequestBody OrderOperateHistoryEntity orderOperateHistory){
		orderOperateHistoryService.updateById(orderOperateHistory);

        return ResponseVo.ok();
    }

    /**
     * 删除
     */
    @PostMapping("/delete")
    @ApiOperation("删除")
    public ResponseVo delete(@RequestBody List<Long> ids){
		orderOperateHistoryService.removeByIds(ids);

        return ResponseVo.ok();
    }

}
