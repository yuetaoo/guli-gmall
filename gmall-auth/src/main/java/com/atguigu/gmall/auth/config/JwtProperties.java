package com.atguigu.gmall.auth.config;

import com.atguigu.gmall.common.utils.RsaUtils;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import javax.annotation.PostConstruct;
import java.io.File;
import java.security.PrivateKey;
import java.security.PublicKey;


@ConfigurationProperties("jwt")//需要@EnableConfigurationProperties启用配置类
@Data
public class JwtProperties {

    private String pubKeyPath;
    private String priKeyPath;
    private String secret;
    private Integer expire;
    private String cookieName;
    private String nickname;

    private PublicKey pubKey;
    private PrivateKey priKey;

    @PostConstruct
    public void init(){
        try {
            File pubFile = new File(pubKeyPath);
            File priFile = new File(priKeyPath);
            //判断公钥或私钥文件是否存在
            if(!pubFile.exists() || !priFile.exists()){
                RsaUtils.generateKey(pubKeyPath, priKeyPath, secret);
            }
                //文件存在时赋值给公钥私钥
            this.pubKey = RsaUtils.getPublicKey(pubKeyPath);
            this.priKey = RsaUtils.getPrivateKey(priKeyPath);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
