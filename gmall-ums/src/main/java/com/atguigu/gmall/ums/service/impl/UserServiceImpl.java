package com.atguigu.gmall.ums.service.impl;

import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.ums.entity.UserEntity;
import com.atguigu.gmall.ums.mapper.UserMapper;
import com.atguigu.gmall.ums.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Date;
import java.util.List;
import java.util.UUID;


@Service("userService")
public class UserServiceImpl extends ServiceImpl<UserMapper, UserEntity> implements UserService {

    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<UserEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<UserEntity>()
        );

        return new PageResultVo(page);
    }

    @Override
    public Boolean checkData(String data, Integer type) {
        QueryWrapper<UserEntity> queryWrapper = new QueryWrapper<>();
        switch (type){
            case 1: queryWrapper.eq("username",data); break;
            case 2: queryWrapper.eq("phone",data); break;
            case 3: queryWrapper.eq("email",data); break;
            default:
                return null;
        }
        UserEntity user = this.getOne(queryWrapper);
        return user == null;
    }

    @Override
    public void register(UserEntity user, String code) {
        //验证短信验证码 TODO

        //生成盐
        String salt = StringUtils.substring(UUID.randomUUID().toString(), 0, 6);
        user.setSalt(salt);

        //对密码加盐
        String md5HexPassword = DigestUtils.md5Hex(user.getPassword() + salt);
        user.setPassword(md5HexPassword);

        //注册用户
        user.setLevelId(1l);
        user.setSourceType(1);
        user.setIntegration(1000);
        user.setGrowth(1000);
        user.setStatus(1);
        user.setCreateTime(new Date());
        user.setNickname(user.getUsername());
        this.save(user);

        //删除短信验证码 TODO

    }

    @Override
    public UserEntity queryUser(String loginName, String password) {

        //先根据登录名查询用户
        List<UserEntity> userList = this.list(new QueryWrapper<UserEntity>().or(Wrapper -> {
            //查询条件中有一个符合就返回
            Wrapper.eq("username", loginName).or().eq("phone", loginName).or().eq("email", loginName);
        }));

        if(CollectionUtils.isEmpty(userList)){
            return null;
            //throw new RuntimeException("用户名或密码输入不合法");//接口可能被其它服务调用，不要抛出异常
        }

        //验证密码
        for (UserEntity user :userList) {
            String pwd = DigestUtils.md5Hex(password + user.getSalt());
            if(StringUtils.equals(user.getPassword(), pwd)){
                return user;
            }
        }
        return null;
    }

}