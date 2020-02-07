package com.atguigu.gmall.order.config;

import com.atguigu.core.utils.RsaUtils;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import javax.annotation.PostConstruct;
import java.security.PublicKey;

@ConfigurationProperties(prefix = "jwt.token")
@Data
public class JwtProperties {

    private String pubKeyPath;
    private String cookieName;

    private String userKey;
    private Integer expireTime;

    private PublicKey publicKey;

    @PostConstruct
    public void init(){
        try {
            publicKey = RsaUtils.getPublicKey(pubKeyPath);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
