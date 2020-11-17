package com.atguigu.gmall.auth.service.impl;

import com.atguigu.gmall.auth.config.JwtProperties;
import com.atguigu.gmall.auth.feign.GmallUmsClient;
import com.atguigu.gmall.auth.service.AuthService;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.common.exception.UserException;
import com.atguigu.gmall.common.utils.CookieUtils;
import com.atguigu.gmall.common.utils.IpUtils;
import com.atguigu.gmall.common.utils.JwtUtils;
import com.atguigu.gmall.ums.entity.UserEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;

@EnableConfigurationProperties({JwtProperties.class})
@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private GmallUmsClient umsClient;

    @Autowired
    private JwtProperties jwtProperties;

    @Override
    public void login(String loginName, String password, HttpServletRequest request, HttpServletResponse response) {
        //调用ums查询用户信息
        ResponseVo<UserEntity> entityResponseVo = umsClient.queryUser(loginName, password);
        UserEntity userEntity = entityResponseVo.getData();

        //判断用户信息是否为空
        if(userEntity == null){
            throw new UserException("用户名或密码错误！");
        }

        try {
            //生成jwt
            HashMap<String, Object> map = new HashMap<>();
            map.put("userId", userEntity.getId());
            map.put("username",userEntity.getUsername());
            map.put("ip", IpUtils.getIpAddressAtService(request));
            String token = JwtUtils.generateToken(map, jwtProperties.getPriKey(), jwtProperties.getExpire());

            //jwt放入cookie 。 参数4不允许在浏览器解析
            CookieUtils.setCookie(request, response, jwtProperties.getCookieName(), token, jwtProperties.getExpire() * 60);
            //存入用户昵称到cookie
            CookieUtils.setCookie(request, response, jwtProperties.getNickname(), userEntity.getNickname(), jwtProperties.getExpire() * 60);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
