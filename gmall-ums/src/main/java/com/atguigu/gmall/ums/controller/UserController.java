package com.atguigu.gmall.ums.controller;

import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.ums.entity.UserEntity;
import com.atguigu.gmall.ums.service.UserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户表
 *
 * @author Tao
 * @email fengge@atguigu.com
 * @date 2020-10-29 10:47:33
 */
@Api(tags = "用户表 管理")
@RestController
@RequestMapping("ums/user")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("check/{data}/{type}")
    public ResponseVo<Boolean> checkData(@PathVariable("data")String data,
                                         @PathVariable("type")Integer type){
        Boolean flag = userService.checkData(data, type);
        return ResponseVo.ok(flag);
    }

    @PostMapping("register")//注册
    public ResponseVo register(UserEntity user, @RequestParam("code")String code){
        userService.register(user, code);
        return ResponseVo.ok();
    }

    @GetMapping("query")
    public ResponseVo<UserEntity> queryUser(@RequestParam("loginName")String loginName,
                                            @RequestParam("password")String password){
        UserEntity user = this.userService.queryUser(loginName, password);
        return ResponseVo.ok(user);
    }

    /**
     * 列表
     */
    @GetMapping
    @ApiOperation("分页查询")
    public ResponseVo<PageResultVo> queryUserByPage(PageParamVo paramVo){
        PageResultVo pageResultVo = userService.queryPage(paramVo);

        return ResponseVo.ok(pageResultVo);
    }


    /**
     * 信息
     */
    @GetMapping("{id}")
    @ApiOperation("详情查询")
    public ResponseVo<UserEntity> queryUserById(@PathVariable("id") Long id){
		UserEntity user = userService.getById(id);

        return ResponseVo.ok(user);
    }

    /**
     * 保存
     */
    @PostMapping
    @ApiOperation("保存")
    public ResponseVo<Object> save(@RequestBody UserEntity user){
		userService.save(user);

        return ResponseVo.ok();
    }

    /**
     * 修改
     */
    @PostMapping("/update")
    @ApiOperation("修改")
    public ResponseVo update(@RequestBody UserEntity user){
		userService.updateById(user);

        return ResponseVo.ok();
    }

    /**
     * 删除
     */
    @PostMapping("/delete")
    @ApiOperation("删除")
    public ResponseVo delete(@RequestBody List<Long> ids){
		userService.removeByIds(ids);

        return ResponseVo.ok();
    }

}
