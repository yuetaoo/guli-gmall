package com.atguigu.gmall.gateway.config;

import com.atguigu.gmall.common.utils.RsaUtils;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import javax.annotation.PostConstruct;
import java.security.PublicKey;


@ConfigurationProperties("jwt")//需要@EnableConfigurationProperties启用配置类
@Data
public class JwtProperties {

    private String pubKeyPath;
    private String cookieName;

    private PublicKey pubKey;

    @PostConstruct
    public void init(){
        try {
            this.pubKey = RsaUtils.getPublicKey(pubKeyPath);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
